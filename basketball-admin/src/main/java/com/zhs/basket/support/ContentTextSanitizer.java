package com.zhs.basket.support;

import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.StringUtils;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 公告正文保存富文本，去掉脚本、嵌入框架和事件属性。
 */
public final class ContentTextSanitizer {

    private static final int CONTENT_MAX = 200_000;
    private static final Pattern SCRIPT = Pattern.compile("(?is)<script[^>]*>.*?</script>");
    private static final Pattern STYLE = Pattern.compile("(?is)<style[^>]*>.*?</style>");
    private static final Pattern IFRAME = Pattern.compile("(?is)<iframe[^>]*>.*?</iframe>");
    private static final Pattern OBJECT = Pattern.compile("(?is)<object[^>]*>.*?</object>");
    private static final Pattern EMBED = Pattern.compile("(?is)<embed\\b[^>]*>");
    private static final Pattern EVENT_ATTR = Pattern.compile("(?i)\\s+on[a-z]+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)");
    private static final Pattern JS_PROTOCOL = Pattern.compile("(?i)javascript:");
    private static final Pattern TAG = Pattern.compile("<[^>]+>");

    private ContentTextSanitizer() {}

    public static String requireRichText(String content) {
        if (isBlankHtml(content)) {
            throw new ServiceException("公告正文不能为空");
        }
        String cleaned = sanitize(content);
        if (isBlankHtml(cleaned)) {
            throw new ServiceException("公告正文不能为空");
        }
        if (cleaned.length() > CONTENT_MAX) {
            throw new ServiceException("公告正文过长");
        }
        return cleaned;
    }

    public static String optionalRichText(String content) {
        if (isBlankHtml(content)) {
            return "";
        }
        String cleaned = sanitize(content);
        if (cleaned.length() > CONTENT_MAX) {
            throw new ServiceException("公告正文过长");
        }
        return isBlankHtml(cleaned) ? "" : cleaned;
    }

    private static String sanitize(String html) {
        String cleaned = html;
        cleaned = SCRIPT.matcher(cleaned).replaceAll("");
        cleaned = STYLE.matcher(cleaned).replaceAll("");
        cleaned = IFRAME.matcher(cleaned).replaceAll("");
        cleaned = OBJECT.matcher(cleaned).replaceAll("");
        cleaned = EMBED.matcher(cleaned).replaceAll("");
        cleaned = EVENT_ATTR.matcher(cleaned).replaceAll("");
        cleaned = JS_PROTOCOL.matcher(cleaned).replaceAll("");
        return cleaned.trim();
    }

    private static boolean isBlankHtml(String content) {
        if (StringUtils.isEmpty(content)) {
            return true;
        }
        if (content.toLowerCase(Locale.ROOT).contains("<img")) {
            return false;
        }
        String text = TAG.matcher(content).replaceAll("")
                .replace("&nbsp;", " ")
                .replace("\u00a0", " ")
                .trim();
        return StringUtils.isEmpty(text);
    }
}
