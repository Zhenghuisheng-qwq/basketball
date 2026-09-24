package com.zhs.config;

import com.zhs.support.ClientIpResolver;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

@Data
@Component
@ConfigurationProperties(prefix = "web.client-ip")
public class WebClientIpProperties {

    /** Comma-separated trusted reverse proxies. Only these remoteAddr values may use XFF. */
    private String trustedProxies = "";

    public Set<String> trustedProxySet() {
        return ClientIpResolver.parseCsv(trustedProxies);
    }
}