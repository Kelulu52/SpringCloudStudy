package com.king.order.properties;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "order")//无需@RefreshScope
@Data
public class OrderProperties {
    String timeout;
    String autoConfirm;
    String dbUrl;
}
