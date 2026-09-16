package com.zhs.util;

import org.springframework.util.StringUtils;

/**
 * 统一脱敏工具
 */
public final class DataMaskUtil {

    private DataMaskUtil() {
    }

    public static String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];
        String maskedLocal;
        if (local.length() >= 6) {
            maskedLocal = local.substring(0, 3) + "****" + local.substring(local.length() - 3);
        } else if (local.length() >= 3) {
            maskedLocal = local.charAt(0) + "****" + local.charAt(local.length() - 1);
        } else if (local.length() == 2) {
            maskedLocal = local.charAt(0) + "****";
        } else {
            maskedLocal = "****";
        }
        return maskedLocal + "@" + domain;
    }

    public static String maskMobile(String mobile) {
        if (!StringUtils.hasText(mobile) || mobile.length() < 7) {
            return "***";
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
    }

    public static String maskIdCard(String idCard) {
        if (!StringUtils.hasText(idCard) || idCard.length() < 8) {
            return "****";
        }
        return idCard.substring(0, 3) + "***********" + idCard.substring(idCard.length() - 4);
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

    public static String maskUa(String ua) {
        if (!StringUtils.hasText(ua)) {
            return "";
        }
        return ua.length() > 80 ? ua.substring(0, 80) + "..." : ua;
    }
}
