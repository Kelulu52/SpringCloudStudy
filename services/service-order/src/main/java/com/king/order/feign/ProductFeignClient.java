package com.king.order.feign;

import com.king.order.feign.fallback.ProductFeignFallBack;
import com.king.product.bean.Product;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;


@FeignClient(value = "service-product",fallback = ProductFeignFallBack.class)
public interface ProductFeignClient {
    //这里是发送get请求
    @GetMapping("/product/{id}")
    Product getProductById(@PathVariable("id") Long id);
}
