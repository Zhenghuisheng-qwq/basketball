package com.zhs.support;

import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * 邮件与请求语言。只认显式 locale、X-CNPA-Locale 和 Accept-Language。
 * 不根据邮箱或 IP 猜测。未传时保持 zh-CN。
 */
public final class MemberLocaleSupport {

    public static final String ZH_CN = "zh-CN";
    public static final String EN_US = "en-US";

    private MemberLocaleSupport() {
    }

    public static String resolve(String explicitLocale, String headerLocale, String acceptLanguage) {
        String raw = firstText(explicitLocale, headerLocale, acceptLanguage);
        if (!StringUtils.hasText(raw)) {
            return ZH_CN;
        }
        String primary = raw.split(",")[0].trim().toLowerCase(Locale.ROOT);
        if (primary.startsWith("en")) {
            return EN_US;
        }
        return ZH_CN;
    }

    public static boolean isEnglish(String locale) {
        return EN_US.equals(resolve(locale, null, null));
    }

    private static String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
