package com.solace.demo.cache;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties("cache")
public class CacheProperties {
    private String requestTopic;
    private String name;
    private int maxMsgsPerTopic;
    private int maxMsgAge;
    private int timeout=5000;
}
