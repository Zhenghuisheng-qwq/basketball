package com.zhs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "web.rate-limit")
public class WebRateLimitProperties {

    private boolean enabled = true;
    private int publicNormalLimit = 180;
    private int publicHighCostLimit = 60;
    private int userWriteLimit = 30;
    private int authLimit = 40;
    private int windowSeconds = 60;

    public int limitOf(String group) {
        return switch (group) {
            case "PUBLIC_HIGH_COST" -> publicHighCostLimit;
            case "USER_WRITE" -> userWriteLimit;
            case "AUTH" -> authLimit;
            default -> publicNormalLimit;
        };
    }
}