package com.king.order.controller;

import com.king.order.bean.Order;
import com.king.order.properties.OrderProperties;
import com.king.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
//@RefreshScope//激活配置属性的自动刷新
@RestController
public class OrderController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderProperties orderProperties;

    @GetMapping("/config")
    public String config(){
        return "orderTimeout:"+orderProperties.getTimeout()+ "," +
                "OrderAutoConfirm:"+orderProperties.getAutoConfirm()+","+
                "dbUrl:"+orderProperties.getDbUrl();
    }
    @GetMapping("/create")
    public Order creatOrder(@RequestParam("userId") Long userId,
                           @RequestParam("productId") Long productId) {
        Order order = orderService.createOrder(productId, userId);
        return order;
    }
}
