package com.zhs.basket.support;

import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.StringUtils;

import java.net.URI;
import java.util.Locale;

/**
 * 外链白名单校验：仅允许 http/https，禁止 javascript:。
 */
public final class ExternalUrlValidator {

    private ExternalUrlValidator() {}

    public static void validateHttpUrl(String url, String fieldLabel) {
        if (StringUtils.isEmpty(url)) {
            return;
        }
        String trimmed = url.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("javascript:")) {
            throw new ServiceException(fieldLabel + "禁止 javascript: 协议");
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
