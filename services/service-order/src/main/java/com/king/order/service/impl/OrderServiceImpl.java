package com.king.order.service.impl;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;


import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.king.order.bean.Order;
import com.king.order.feign.ProductFeignClient;
import com.king.order.service.OrderService;
import com.king.product.bean.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private DiscoveryClient discoveryClient;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private LoadBalancerClient loadBalancerClient;
    @Autowired
    private ProductFeignClient productFeignClient;
    //SentinelResource异常处理
    @SentinelResource(value = "creatOrder",blockHandler = "createOrderFallBack")
    @Override
    public Order createOrder(Long productId, Long userId) {
//        Product product = getProductFromRemoteWithAnnotationBalance(productId);
        Product product = productFeignClient.getProductById(productId);
        Order order = new Order();
        order.setId(1L);
        //总金额
        order.setTotalAmount(product.getPrice().multiply(new BigDecimal(product.getNum())));
        order.setUserId(userId);
        order.setNickName("鹏业");
        order.setAddress("上海");
        //TODO
        order.setProductlist(Arrays.asList(product));
        return order;
    }

    public Order createOrderFallBack(Long productId, Long userId, BlockException blockException) {
        Order order = new Order();
        order.setId(0L);
        order.setTotalAmount(new BigDecimal("0"));
        order.setUserId(userId);
        order.setNickName("未知信息");
        order.setAddress("异常信息："+blockException.getClass());

        return order;
    }
    public Product getProductFromRemote(Long productId){
        List<ServiceInstance> instances = discoveryClient.getInstances("service-product");
        ServiceInstance serviceInstance = instances.get(0);
        String url="http://"+serviceInstance.getHost()+":"+serviceInstance.getPort()+"/product/"+productId;
        log.info("远程请求:{}",url);
        Product forObject = restTemplate.getForObject(url, Product.class);
        return forObject;
    }
    public Product getProductFromRemoteWithBalance(Long productId){
        ServiceInstance choose = loadBalancerClient.choose("service-product");
        String url="http://"+choose.getHost()+":"+choose.getPort()+"/product/"+productId;
        log.info("远程请求:{}",url);
        Product forObject = restTemplate.getForObject(url, Product.class);
        return forObject;
    }
    //基于注解的负载均衡
    public Product getProductFromRemoteWithAnnotationBalance(Long productId){
        String url="http://service-product/product/"+productId;
        Product forObject = restTemplate.getForObject(url, Product.class);
        return forObject;
    }
}
