package com.zhs.basket.support;

/**
 * 通知模板参数 JSON。尖括号写成 Unicode 转义，避免参数被当成 HTML。
 */
public final class NoticeParams {

    private NoticeParams() {
    }

    public static String json(String... pairs) {
        if (pairs == null || pairs.length == 0) {
            return null;
        }
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("template params must be key/value pairs");
        }
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        for (int i = 0; i < pairs.length; i += 2) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append('"').append(escape(pairs[i])).append('"');
            builder.append(':');
            builder.append('"').append(escape(pairs[i + 1])).append('"');
        }
        builder.append('}');
        return builder.toString();
    }

    public static String escape(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            switch (ch) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '<' -> builder.append("\\u003c");
                case '>' -> builder.append("\\u003e");
                case '&' -> builder.append("\\u0026");
                default -> builder.append(ch);
            }
        }
        return builder.toString();
    }
}
