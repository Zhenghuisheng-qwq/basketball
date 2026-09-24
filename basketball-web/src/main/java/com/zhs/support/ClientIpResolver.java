package com.zhs.support;

import com.zhs.config.WebClientIpProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class ClientIpResolver {

    private static final Set<String> LOCAL_LOOPBACK = Set.of(
            "127.0.0.1",
            "::ffff:127.0.0.1",
            "::1",
            "localhost"
    );

    private final WebClientIpProperties properties;

    public ClientIpResolver(WebClientIpProperties properties) {
        this.properties = properties;
    }

    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String remoteAddr = normalizeIp(request.getRemoteAddr());
        if (!isTrustedProxy(remoteAddr)) {
            return remoteAddr.isEmpty() ? "unknown" : remoteAddr;
        }
        String forwarded = firstForwardedIp(request.getHeader("X-Forwarded-For"));
        if (StringUtils.hasText(forwarded)) {
            return forwarded;
        }
        String realIp = normalizeIp(request.getHeader("X-Real-IP"));
        if (StringUtils.hasText(realIp)) {
            return realIp;
        }
        return remoteAddr.isEmpty() ? "unknown" : remoteAddr;
    }

    public boolean isTrustedProxy(String remoteAddr) {
        String normalized = normalizeIp(remoteAddr);
        if (normalized.isEmpty()) {
            return false;
        }
        if (LOCAL_LOOPBACK.contains(normalized)) {
            return true;
        }
        for (String proxy : properties.trustedProxySet()) {
            if (normalized.equals(normalizeIp(proxy))) {
                return true;
            }
        }
        return false;
    }

    static String firstForwardedIp(String xff) {
        if (!StringUtils.hasText(xff)) {
            return "";
        }
        return normalizeIp(xff.split(",")[0]);
    }

    static String normalizeIp(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return "";
        }
        if (value.startsWith("[") && value.contains("]")) {
            value = value.substring(1, value.indexOf(']'));
        }
        int zone = value.indexOf('%');
        if (zone >= 0) {
            value = value.substring(0, zone);
        }
        if (value.startsWith("::ffff:")) {
            value = value.substring("::ffff:".length());
        }
        return value.toLowerCase(Locale.ROOT);
    }

    public static Set<String> parseCsv(String csv) {
        Set<String> values = new LinkedHashSet<>();
        if (!StringUtils.hasText(csv)) {
            return values;
        }
        for (String part : csv.split(",")) {
            String normalized = normalizeIp(part);
            if (!normalized.isEmpty()) {
                values.add(normalized);
            }
        }
        return values;
    }
}