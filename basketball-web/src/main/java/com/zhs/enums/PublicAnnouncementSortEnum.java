package com.zhs.enums;

import com.zhs.exception.ServiceException;

/**
 * 公开公告列表排序白名单。
 */
public enum PublicAnnouncementSortEnum {
    PUBLISH_TIME("publishTime"),
    VIEW_COUNT("viewCount");

    private final String code;

    PublicAnnouncementSortEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static PublicAnnouncementSortEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return PUBLISH_TIME;
        }
        String normalized = code.trim();
        for (PublicAnnouncementSortEnum v : values()) {
            if (v.name().equalsIgnoreCase(normalized) || v.code.equalsIgnoreCase(normalized)) {
                return v;
            }
        }
        throw new ServiceException("非法公告排序字段: " + code);
    }
}
