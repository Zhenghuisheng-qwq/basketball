package com.zhs.basket.support;

import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.StringUtils;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

/**
 * 外链白名单校验：仅允许 http/https，禁止 javascript/data/file/vbscript。
 */
public final class ExternalUrlValidator {

    public static final int DEFAULT_MAX_LENGTH = 512;

    private static final Set<String> FORBIDDEN_SCHEMES = Set.of("javascript", "data", "file", "vbscript");

    private ExternalUrlValidator() {}

    public static void validateHttpUrl(String url, String fieldLabel) {
        validateHttpUrl(url, fieldLabel, 0);
    }

    public static void validateHttpUrl(String url, String fieldLabel, int maxLength) {
        if (StringUtils.isEmpty(url)) {
            return;
        }
        String trimmed = url.trim();
        if (maxLength > 0 && trimmed.length() > maxLength) {
            throw new ServiceException(fieldLabel + "长度不能超过" + maxLength);
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        for (String forbidden : FORBIDDEN_SCHEMES) {
            if (lower.startsWith(forbidden + ":")) {
                throw new ServiceException(fieldLabel + "禁止 " + forbidden + ": 协议");
            }
        }
        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme();
            if (scheme == null
                    || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                throw new ServiceException(fieldLabel + "仅允许 http/https 链接");
            }
        } catch (IllegalArgumentException ex) {
            throw new ServiceException(fieldLabel + "格式非法");
        }
    }
}
