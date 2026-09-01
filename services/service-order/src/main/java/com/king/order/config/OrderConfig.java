package com.king.order.config;

import feign.Logger;
import feign.Retryer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
@Configuration
public class OrderConfig {
    //@Bean
    Retryer retryer(){
        return new Retryer.Default();
    }


    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
    @LoadBalanced//自动负载均衡
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
