package com.king.order.service.impl;
import java.math.BigDecimal;


import com.king.order.bean.Order;
import com.king.order.service.OrderService;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceImpl implements OrderService {
    @Override
    public Order createOrder(Long productId, Long userId) {
        Order order = new Order();
        order.setId(1L);
        //TODO
        order.setTotalAmount(new BigDecimal("0"));
        order.setUserId(userId);
        order.setNickName("鹏业");
        order.setAddress("上海");
        //TODO
        order.setProductlist(null);
        return order;
    }
}
