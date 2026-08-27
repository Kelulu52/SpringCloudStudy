package com.king.order.service;

import com.king.order.bean.Order;

public interface OrderService {
    Order createOrder(Long productId, Long userId);
}
