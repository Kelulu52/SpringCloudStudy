package com.king.order.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
@Component
public class XTokenReqquestInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate requestTemplate) {
        System.out.println("拦截器起启动");
        requestTemplate.header("X-Token", UUID.randomUUID().toString());
    }
}
