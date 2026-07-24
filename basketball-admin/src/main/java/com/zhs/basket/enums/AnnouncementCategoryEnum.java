package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

/**
 * basket_announcement.category_code（无独立分类表）。
 */
public enum AnnouncementCategoryEnum {
    NOTICE("NOTICE", "通知公告"),
    RULE("RULE", "规则说明"),
    EVENT("EVENT", "赛事资讯"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String description;

    AnnouncementCategoryEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static AnnouncementCategoryEnum fromCode(String code) {
        if (code == null || code.isBlank()) throw new ServiceException("未知公告分类");
        for (AnnouncementCategoryEnum v : values()) {
            if (v.code.equalsIgnoreCase(code.trim())) return v;
        }
        throw new ServiceException("未知公告分类: " + code);
    }
}
