package com.zhs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "web.cors")
public class WebCorsProperties {

    /** Comma-separated origins. Wildcard is rejected. Prod uses CNPA_CORS_ALLOWED_ORIGINS. */
    private String allowedOrigins = "";

    /** Bearer token does not need cookies. Default false. */
    private boolean allowCredentials = false;

    public List<String> allowedOriginList() {
        List<String> origins = new ArrayList<>();
        if (!StringUtils.hasText(allowedOrigins)) {
            return origins;
        }
        for (String part : allowedOrigins.split(",")) {
            String origin = part.trim();
            if (!origin.isEmpty() && !"*".equals(origin)) {
                origins.add(origin);
            }
        }
        return origins;
    }
}