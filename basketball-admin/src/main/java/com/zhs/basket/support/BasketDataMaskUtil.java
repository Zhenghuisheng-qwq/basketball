package com.zhs.basket.support;

import org.springframework.util.StringUtils;

public final class BasketDataMaskUtil {

    private BasketDataMaskUtil() {
    }

    public static String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String visible = local.length() <= 2 ? local.charAt(0) + "*" : local.substring(0, 2) + "***";
        return visible + "@" + parts[1];
    }

    public static String maskMobile(String mobile) {
        if (!StringUtils.hasText(mobile) || mobile.length() < 7) {
            return "***";
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
    }

    public static String maskIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return "***";
        }
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + ".*.*";
        }
        if (ip.length() <= 4) {
            return "***";
        }
        return ip.substring(0, Math.min(6, ip.length())) + "***";
    }
}
