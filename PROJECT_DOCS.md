# Cloud-Demo 项目文档

## 一、项目概述

本项目是一个基于 **Spring Boot 3.3.4** + **Spring Cloud 2023.0.3** + **Spring Cloud Alibaba 2023.0.3.2** 的微服务教学演示项目，使用 Java 21 开发。项目涵盖了 Spring Cloud 微服务生态中的核心功能：API 网关、服务注册与发现、负载均衡、OpenFeign 远程调用、Sentinel 流量治理、Nacos 配置中心，以及使用 Seata 实现的分布式事务。

### 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.3.4 | 基础框架 |
| Spring Cloud | 2023.0.3 | 微服务套件 |
| Spring Cloud Alibaba | 2023.0.3.2 | Nacos / Seata / Sentinel |
| Nacos | - | 服务注册与发现、配置中心 |
| Spring Cloud Gateway | - | API 网关 |
| OpenFeign | - | 声明式 HTTP 客户端 |
| Spring Cloud LoadBalancer | - | 客户端负载均衡 |
| Sentinel | - | 流量控制、熔断降级 |
| Seata | - | 分布式事务 |
| MyBatis | 3.0.4 | 持久层框架 |
| MySQL | - | 关系型数据库 |
| Lombok | - | 简化 Java 代码 |

---

## 二、项目模块结构

```
cloud-demo (根 POM, packaging=pom)
├── gateway                    # API 网关模块
├── model                      # 公共实体类模块
└── services (子 POM, packaging=pom)
    ├── service-order          # 订单服务（基础微服务演示）
    ├── service-product        # 商品服务（基础微服务演示）
    ├── seata-business         # 业务服务（Seata 分布式事务协调者）
    ├── seata-order            # Seata 订单服务
    ├── seata-storage          # Seata 库存服务
    └── seata-account          # Seata 账户服务
```

> **注意：** 根 POM 的 `<modules>` 列表中未显式声明 seata-* 子模块，但 services 父 POM 中已声明。service-order 和 service-product 同时出现在根 POM 和 services POM 的 modules 中（重复声明）。

---

## 三、模块详细说明

### 3.1 model — 公共实体模块

**坐标：** `org.example:model:0.0.1-SNAPSHOT`

该模块不包含任何 Spring 依赖，仅提供纯 Java 实体类和通用响应封装。

| 文件 | 包路径 | 说明 |
|------|--------|------|
| `R.java` | `com.king.common` | 统一响应封装类，包含 `code`、`msg`、`data` 三个字段，提供 `ok()` / `error()` 静态工厂方法 |
| `Order.java` | `com.king.order.bean` | 订单实体，字段：`id`, `totalAmount`(BigDecimal), `userId`, `nickName`, `address`, `productlist`(List\<Product\>) |
| `Product.java` | `com.king.product.bean` | 商品实体，字段：`id`, `price`(BigDecimal), `productName`, `num` |
| `xx.java` | `com.king.order` | 空类（占位/测试用） |
| `xx.java` | `com.king.product` | 空类（占位/测试用） |

---

### 3.2 gateway — API 网关模块

**坐标：** `org.example:gateway:0.0.1-SNAPSHOT`
**端口：** `80`
**服务名：** `gateway`
**注册中心：** Nacos `127.0.0.1:8848`

**核心依赖：** Spring Cloud Gateway、LoadBalancer、Nacos Discovery

#### 启动类

| 文件 | 说明 |
|------|------|
| `GateWayApplication.java` | `@SpringBootApplication` 标准启动入口 |

#### 自定义全局过滤器

| 文件 | 说明 |
|------|------|
| `RtGlobalFilter.java` | 实现 `GlobalFilter + Ordered`，记录每个请求的 URL、开始时间、结束时间和耗时。优先级为 `0`。使用 Reactor 的 `doFinally` 回调在响应完成后记录日志。 |

#### 自定义网关过滤器工厂

| 文件 | 说明 |
|------|------|
| `OneceTokenGatewayFilterFactory.java` | 继承 `AbstractNameValueGatewayFilterFactory`，在响应完成后（`then(Mono.fromRunnable(...))`）向响应 Header 中写入 Token。支持两种模式：`uuid` 模式生成随机 UUID，`jwt` 模式写入一个硬编码的 JWT 字符串。配置格式：`OneceToken=HeaderName,value` |

#### 自定义断言工厂

| 文件 | 说明 |
|------|------|
| `VipRoutePredicateFactory.java` | 继承 `AbstractRoutePredicateFactory`，根据请求参数是否等于指定值来匹配路由。Config 类包含 `param`（参数名）和 `value`（期望值），两者均使用 `@NotEmpty` 校验。shortcutFieldOrder 为 `["param", "value"]` |

#### 路由配置（application-route.yml）

```yaml
routes:
  # 路由1：转发到必应搜索（演示外部路由）
  - id: bing-route
    uri: https://cn.bing.com/
    predicates:
      - Path=/search
      - Query=q, haha          # 查询参数 q 匹配正则 haha
      - Vip=user, pengye       # 自定义断言：参数 user=pengye

  # 路由2：转发到 service-order（演示服务路由）
  - id: order-route
    uri: lb://service-order    # lb:// 前缀表示负载均衡
    predicates:
      - Path=/api/order/**
    filters:
      - RewritePath=/api/order/?(?<segment>.*), /${segment}  # 路径重写
      - OneceToken=X-Response-Token,jwt                     # 自定义过滤器
    order: 0

  # 路由3：转发到 service-product
  - id: product-route
    uri: lb://service-product
    predicates:
      - Path=/api/product/**
        matchTrailingSlash: true
    filters:
      - RewritePath=/api/product/?(?<segment>.*), /${segment}

# 全局默认过滤器
default-filters:
  - AddResponseHeader=X-Request-red, blue

# 全局 CORS 配置
globalcors:
  '[/**]':
    allowed-origin-patterns: '*'
    allowed-headers: '*'
    allowed-methods: '*'
```

#### 请求流转示例

```
客户端 → Gateway(:80)
  → /api/order/create?userId=1&productId=2
  → 匹配 order-route（Path=/api/order/**）
  → RewritePath 重写为 /create?userId=1&productId=2
  → LoadBalancer 选择 service-order 实例
  → 转发到 service-order:create
  → 响应返回时 OneceToken 过滤器添加 X-Response-Token 响应头
```

---

### 3.3 service-product — 商品服务

**坐标：** `org.example:service-product:0.0.1-SNAPSHOT`
**端口：** `9000`
**服务名：** `service-product`
**注册中心：** Nacos `127.0.0.1:8848`

**核心依赖：** Spring Boot Web、Nacos Discovery、Sentinel

#### 启动类

| 文件 | 说明 |
|------|------|
| `ProductMainApplication.java` | 标准 `@SpringBootApplication` 启动入口 |

#### 控制器

| 文件 | 端点 | 说明 |
|------|------|------|
| `ProductController.java` | `GET /product/{id}` | 根据 ID 查询商品信息，同时读取请求头 `X-Token` 并打印到控制台（用于验证 Feign 拦截器传递的 Token） |

#### 服务层

| 文件 | 说明 |
|------|------|
| `ProductService.java` | 接口，定义 `getById(long productid)` 方法 |
| `ProductServiceImpl.java` | 实现类，返回硬编码的 Product 对象：价格=100，名称=`苹果+{id}`，数量=2 |

#### 配置类

| 文件 | 说明 |
|------|------|
| `ProductServiceConfig.java` | 注册 `RestTemplate` Bean（未标注 `@LoadBalanced`，不用于跨服务调用） |

#### 测试类

| 文件 | 说明 |
|------|------|
| `DiscoveryTest.java` | 单元测试，演示使用 `DiscoveryClient` 和 `NacosServiceDiscovery` 遍历所有已注册服务及其实例信息 |

#### 配置（application.properties）

```properties
spring.application.name=service-product
server.port=9000
spring.cloud.nacos.server-addr=127.0.0.1:8848
spring.cloud.nacos.config.import-check.enabled=false
spring.cloud.sentinel.transport.dashboard=localhost:8080
spring.cloud.sentinel.eager=true
```

---

### 3.4 service-order — 订单服务

**坐标：** `org.example:service-order:0.0.1-SNAPSHOT`
**端口：** `8000`
**服务名：** `service-order`
**注册中心：** Nacos `127.0.0.1:8848`
**Sentinel 控制台：** `localhost:8080`

**核心依赖：** Spring Boot Web、LoadBalancer、Nacos Discovery、Sentinel、OpenFeign

#### 启动类

| 文件 | 说明 |
|------|------|
| `OrderMainApplication.java` | `@SpringBootApplication` + `@EnableFeignClients`。同时注册 `ApplicationRunner` Bean，通过 Nacos `ConfigService` 监听 `service-order.properties` 配置变更，收到变更时打印通知信息 |

#### 控制器

| 文件 | 端点 | 说明 |
|------|------|------|
| `OrderController.java` | `GET /config` | 返回当前 Nacos 配置属性值（timeout, autoConfirm, dbUrl） |
| | `GET /create?userId=&productId=` | 创建订单（通过 Feign 调用 service-product 获取商品信息） |
| | `GET /seckill?userId=&productId=` | 秒杀下单，使用 `@SentinelResource` 注解，降级方法为 `seckillFallback` |
| | `GET /writeDb` | 写数据库演示接口，返回 "Write DB" |
| | `GET /readDb` | 读数据库演示接口，返回 "Read DB" |

#### 服务层

| 文件 | 说明 |
|------|------|
| `OrderService.java` | 接口，定义 `createOrder(Long productId, Long userId)` 方法 |
| `OrderServiceImpl.java` | 实现类。主方法使用 Feign 调用获取商品，组装 Order 对象。同时包含三种远程调用方式的演示方法：(1) `getProductFromRemote` — 使用 `DiscoveryClient` 手动获取实例；(2) `getProductFromRemoteWithBalance` — 使用 `LoadBalancerClient` 负载均衡；(3) `getProductFromRemoteWithAnnotationBalance` — 使用 `@LoadBalanced` RestTemplate 直接按服务名调用。此外还有 `@SentinelResource` 注解的熔断降级方法 `createOrderFallBack` |

#### Feign 客户端

| 文件 | 说明 |
|------|------|
| `ProductFeignClient.java` | `@FeignClient(value = "service-product", fallback = ProductFeignFallBack.class)`，定义 `GET /product/{id}` 映射 |
| `ProductFeignFallBack.java` | 降级实现类，返回 id 相同但 price=0、productName="未知商品"、num=0 的 Product 对象 |

#### Feign 请求拦截器

| 文件 | 说明 |
|------|------|
| `XTokenReqquestInterceptor.java` | 实现 `RequestInterceptor`，每次 Feign 请求自动添加 `X-Token` 请求头，值为随机 UUID |

#### Sentinel 配置

| 文件 | 说明 |
|------|------|
| `MyBlockException.java` | 实现 `BlockExceptionHandler`，当 Sentinel 限流/降级触发时，返回 JSON 格式的错误响应：`{code: 500, msg: "{资源名}被sentinel限制了，原因是：{异常类}"}` |
| `application-feign.yml` | 配置 Feign 的日志级别（FULL）、连接超时（3000ms）、读取超时（5000ms），并启用 Feign + Sentinel 集成（`feign.sentinel.enabled=true`）。Sentinel 控制台地址 `localhost:8080`，启用 `eager=true`（立即启动），`web-context-unify=false` |

#### 配置类

| 文件 | 说明 |
|------|------|
| `OrderConfig.java` | 注册 `@LoadBalanced RestTemplate` Bean（支持按服务名调用），配置 Feign 日志级别为 `Logger.Level.FULL` |
| `OrderProperties.java` | `@ConfigurationProperties(prefix = "order")` 绑定 Nacos 配置，字段：`timeout`, `autoConfirm`, `dbUrl` |

#### 多环境配置（application.yml）

```yaml
server.port: 8000
spring:
  application.name: service-order
  profiles:
    active: dev
    include: feign
  cloud.nacos:
    server-addr: 127.0.0.1:8848
    config:
      namespace: ${spring.profiles.active:public}
      import-check.enabled: false

# 多环境分离配置：
# dev  → 导入 nacos:common.properties?group=order, nacos:database.properties?group=order
# test → 同上
# prod → 同上
```

#### 测试类

| 文件 | 说明 |
|------|------|
| `LoadBalancerTest.java` | 单元测试，演示使用 `LoadBalancerClient.choose("service-product")` 三次，验证负载均衡策略 |

---

### 3.5 Seata 分布式事务模块

四个模块协同实现经典的**电商采购分布式事务**场景：用户发起采购 → 扣减库存 → 创建订单 + 扣减账户余额。使用 Seata AT 模式保证跨服务的最终一致性。

#### 调用链路

```
seata-business (:11000)
  ├── Feign → seata-storage (:13000)  扣减库存
  └── Feign → seata-order   (:12000)  创建订单
                └── Feign → seata-account (:9005)  扣减账户余额
```

**全局事务由 `@GlobalTransactional` 标注在 seata-business 的 purchase 方法上启动。**

---

#### 3.5.1 seata-business — 业务服务（全局事务协调者）

**端口：** `11000` | **服务名：** `seata-business`
**包名：** `com.atguigu.business`

| 文件 | 说明 |
|------|------|
| `SeataBusinessMainApplication.java` | `@SpringBootApplication` + `@EnableDiscoveryClient` + `@EnableFeignClients(basePackages = "com.atguigu.business.feign")` |
| `PurchaseRestController.java` | `GET /purchase?userId=&commodityCode=&count=` — 采购入口，调用 BusinessService.purchase() |
| `BusinessService.java` | 接口，定义 `purchase(String userId, String commodityCode, int orderCount)` |
| `BusinessServiceImpl.java` | **核心实现**：方法标注 `@GlobalTransactional`（Seata 全局事务），依次调用 StorageFeignClient.deduct() → OrderFeignClient.create() |
| `OrderFeignClient.java` | `@FeignClient(value = "seata-order")`，映射 `GET /create` |
| `StorageFeignClient.java` | `@FeignClient(value = "seata-storage")`，映射 `GET /deduct` |

**配置：**
```yaml
spring.application.name: seata-business
spring.cloud.nacos.server-addr: 127.0.0.1:8848
spring.cloud.nacos.config.import-check.enabled: false
server.port: 11000
```

---

#### 3.5.2 seata-storage — 库存服务

**端口：** `13000` | **服务名：** `seata-storage`
**数据库：** `jdbc:mysql://localhost:3306/storage_db`
**包名：** `com.atguigu.storage`

| 文件 | 说明 |
|------|------|
| `SeataStorageMainApplication.java` | `@SpringBootApplication` + `@EnableDiscoveryClient` + `@MapperScan("com.atguigu.storage.mapper")` |
| `StorageRestController.java` | `GET /deduct?commodityCode=&count=` — 扣减库存 |
| `StorageService.java` | 接口，定义 `deduct(String commodityCode, int count)` |
| `StorageServiceImpl.java` | 实现类，标注 `@Transactional`（本地事务），调用 Mapper 执行 `UPDATE storage_tbl SET count = count - ? WHERE commodity_code = ?`。**注意：** 当 `count==5` 时抛出 `RuntimeException("库存不足")`，用于演示 Seata 事务回滚 |
| `StorageTbl.java` | 实体类，字段：`id`(Integer), `commodityCode`(String), `count`(Integer) |
| `StorageTblMapper.java` | Mapper 接口，CRUD + `deduct()` 自定义方法 |
| `StorageTblMapper.xml` | MyBatis XML 映射文件 |

**数据库表：`storage_tbl`**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER | 主键（自增） |
| commodity_code | VARCHAR | 商品编码 |
| count | INTEGER | 库存数量 |

---

#### 3.5.3 seata-order — 订单服务

**端口：** `12000` | **服务名：** `seata-order`
**数据库：** `jdbc:mysql://localhost:3306/order_db`
**包名：** `com.atguigu.order`

| 文件 | 说明 |
|------|------|
| `SeataOrderMainApplication.java` | `@SpringBootApplication` + `@EnableDiscoveryClient` + `@EnableFeignClients` + `@MapperScan("com.atguigu.order.mapper")` |
| `OrderRestController.java` | `GET /create?userId=&commodityCode=&count=` — 创建订单 |
| `OrderService.java` | 接口，定义 `create(String userId, String commodityCode, int orderCount)` → 返回 `OrderTbl` |
| `OrderServiceImpl.java` | **核心实现**：标注 `@Transactional`（本地事务），先计算订单金额（单价固定=9），再 Feign 调用 seata-account 扣减账户余额，最后插入订单记录。**注意：** 插入后执行 `int i=10/0;` 故意触发除零异常，用于演示 Seata 全局事务回滚 |
| `AccountFeignClient.java` | `@FeignClient(value = "seata-account")`，映射 `GET /debit` |
| `OrderTbl.java` | 实体类，字段：`id`(Integer), `userId`(String), `commodityCode`(String), `count`(Integer), `money`(Integer) |
| `OrderTblMapper.java` | Mapper 接口，CRUD 标准方法 |
| `OrderTblMapper.xml` | MyBatis XML 映射文件 |

**数据库表：`order_tbl`**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER | 主键（自增） |
| user_id | VARCHAR | 用户 ID |
| commodity_code | VARCHAR | 商品编码 |
| count | INTEGER | 订单数量 |
| money | INTEGER | 订单金额 |

---

#### 3.5.4 seata-account — 账户服务

**端口：** `9005` | **服务名：** `seata-account`
**数据库：** `jdbc:mysql://localhost:3306/account_db`
**包名：** `com.atguigu.account`

| 文件 | 说明 |
|------|------|
| `SeataAccountMainApplication.java` | `@SpringBootApplication` + `@EnableDiscoveryClient` + `@MapperScan("com.atguigu.account.mapper")` |
| `AccountRestController.java` | `GET /debit?userId=&money=` — 扣减账户余额 |
| `AccountService.java` | 接口，定义 `debit(String userId, int money)` |
| `AccountServiceImpl.java` | 实现类，标注 `@Transactional`（本地事务），调用 Mapper 执行 `UPDATE account_tbl SET money = money - ? WHERE user_id = ?` |
| `AccountTbl.java` | 实体类，字段：`id`(Integer), `userId`(String), `money`(Integer) |
| `AccountTblMapper.java` | Mapper 接口，CRUD + `debit()` 自定义方法 |
| `AccountTblMapper.xml` | MyBatis XML 映射文件 |

**数据库表：`account_tbl`**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER | 主键（自增） |
| user_id | VARCHAR | 用户 ID |
| money | INTEGER | 账户余额 |

---

## 四、分布式事务流程详解

### 4.1 正常流程

```
客户端 → GET /purchase?userId=1&commodityCode=product_1&count=10
       ↓
seata-business (BusinessServiceImpl.purchase)
  [@GlobalTransactional]
       ↓
  ┌─────┴──────────────────────────────┐
  │                                    │
seata-storage                      seata-order
(deduct 扣减库存)                  (create 创建订单)
  │                                    │
  │                              ┌─────┴──────┐
  │                              │            │
  │                         seata-account   写入order_tbl
  │                         (debit 扣减余额)  ← 触发 10/0 异常 → 回滚
  │                              │
  └──────────────────────────────┘
所有参与方事务回滚
```

### 4.2 两处故意异常点（用于演示回滚）

| 位置 | 文件 | 代码 | 说明 |
|------|------|------|------|
| 异常点 1 | `StorageServiceImpl.java:21` | `if(count==5) throw new RuntimeException("库存不足")` | 当 count=5 时库存服务抛出异常 |
| 异常点 2 | `OrderServiceImpl.java:35` | `int i=10/0;` | 每次创建订单后必然触发 ArithmeticException |

### 4.3 Seata AT 模式原理

1. **业务服务**（seata-business）的 `@GlobalTransactional` 方法启动全局事务，向 TC（Transaction Coordinator）注册
2. 各参与服务（storage/order/account）的 `@Transactional` 方法作为分支事务
3. AT 模式下，Seata 通过解析 SQL 自动生成**一阶段**的undo_log（before image / after image）
4. 若全局成功，二阶段自动清理 undo_log；若全局失败，TC 通知各 RM 使用 undo_log 执行回滚

---

## 五、微服务通信方式总结

本项目演示了 **三种** Spring Cloud 中的服务间调用方式：

| 方式 | 实现 | 文件位置 | 说明 |
|------|------|----------|------|
| RestTemplate + DiscoveryClient | `OrderServiceImpl.getProductFromRemote()` | service-order | 手动从注册中心获取实例列表，构造 URL |
| RestTemplate + LoadBalancerClient | `OrderServiceImpl.getProductFromRemoteWithBalance()` | service-order | 手动调用负载均衡器选择实例 |
| RestTemplate + @LoadBalanced | `OrderServiceImpl.getProductFromRemoteWithAnnotationBalance()` | service-order | 直接按服务名调用（最常用） |
| OpenFeign | `ProductFeignClient` / `OrderFeignClient` / `StorageFeignClient` / `AccountFeignClient` | 多处 | 声明式 HTTP 客户端，支持 fallback 降级 |

---

## 六、服务端口总览

| 服务名 | 端口 | 说明 |
|--------|------|------|
| gateway | 80 | API 网关 |
| service-product | 9000 | 商品服务 |
| service-order | 8000 | 订单服务 |
| seata-business | 11000 | 业务服务（全局事务入口） |
| seata-order | 12000 | Seata 订单服务 |
| seata-storage | 13000 | Seata 库存服务 |
| seata-account | 9005 | Seata 账户服务 |

**外部依赖端口：**
- Nacos 注册中心 / 配置中心：`127.0.0.1:8848`
- Sentinel 控制台：`localhost:8080`
- MySQL 数据库：`localhost:3306`（三个库：`order_db`, `storage_db`, `account_db`）

---

## 七、关键功能演示清单

| 功能 | 模块 | 说明 |
|------|------|------|
| API 网关路由 | gateway | 基于 Path 的路由转发、路径重写、外部站点点位路由 |
| 全局 CORS | gateway | 全量允许跨域 |
| 全局过滤器 | gateway | `RtGlobalFilter` 记录请求耗时 |
| 自定义过滤器工厂 | gateway | `OneceTokenGatewayFilterFactory` 向响应头注入 Token |
| 自定义断言工厂 | gateway | `VipRoutePredicateFactory` 基于参数值的路由匹配 |
| 服务注册与发现 | 全部 | 注册到 Nacos，DiscoveryClient 查询 |
| 负载均衡 | service-order | RestTemplate + @LoadBalanced |
| OpenFeign 声明式调用 | service-order, seata-* | 服务间 HTTP 调用 |
| Feign 降级 | service-order | `ProductFeignFallBack` 兜底回调 |
| Feign 请求拦截器 | service-order | `XTokenReqquestInterceptor` 统一添加请求头 |
| Sentinel 流量控制 | service-order | `@SentinelResource` + `MyBlockExceptionHandler` |
| Nacos 配置中心 | service-order | 多环境配置（dev/test/prod），动态配置监听 |
| 配置属性绑定 | service-order | `@ConfigurationProperties` + `OrderProperties` |
| 分布式事务 | seata-* | Seata AT 模式，`@GlobalTransactional` + `@Transactional` |
