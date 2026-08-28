package com.king.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;

@SpringBootTest
public class LoadBalancerTest {
    @Autowired
    private LoadBalancerClient loadBalancerClient;
    @Test
    public void test() {
        ServiceInstance choose = loadBalancerClient.choose("service-product");
        System.out.println("choose: " + choose.getHost() + ":" + choose.getPort());

        ServiceInstance choose2 = loadBalancerClient.choose("service-product");
        System.out.println("choose: " + choose2.getHost() + ":" + choose2.getPort());

        ServiceInstance choose3 = loadBalancerClient.choose("service-product");
        System.out.println("choose: " + choose3.getHost() + ":" + choose3.getPort());
    }
}
