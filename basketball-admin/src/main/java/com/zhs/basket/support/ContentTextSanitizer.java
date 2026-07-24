package com.zhs.basket.support;

import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.StringUtils;

/**
 * 公告正文按纯文本处理：禁止 HTML 标签字符。
 */
public final class ContentTextSanitizer {

    private ContentTextSanitizer() {}

    public static String requirePlainText(String content) {
        if (StringUtils.isEmpty(content)) {
            throw new ServiceException("公告正文不能为空");
        }
        String text = content.trim();
        if (text.indexOf('<') >= 0 || text.indexOf('>') >= 0) {
            throw new ServiceException("公告正文仅支持纯文本，不能包含 HTML 标签字符");
        }
        return text;
    }

    public static String optionalPlainText(String content) {
        if (StringUtils.isEmpty(content)) {
            return content;
        }
        return requirePlainText(content);
    }
}
