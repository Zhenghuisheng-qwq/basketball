package com.zhs.i18n;

import org.springframework.util.StringUtils;

/**
 * 有模板码时使用对应语言渲染结果；历史消息没有模板码时保留原文。
 */
public final class NoticeDisplay {

    private NoticeDisplay() {
    }

    public static String text(String templateCode, String storedText, String renderedText) {
        if (!StringUtils.hasText(templateCode) || !StringUtils.hasText(renderedText)) {
            return storedText;
        }
        return renderedText;
    }
}
