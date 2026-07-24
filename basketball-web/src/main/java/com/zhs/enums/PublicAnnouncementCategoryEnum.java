package com.zhs.enums;

/**
 * 公告分类（无独立分类表）：NOTICE / RULE / EVENT / OTHER。
 */
public enum PublicAnnouncementCategoryEnum {
    NOTICE("NOTICE", "通知公告"),
    RULE("RULE", "规则说明"),
    EVENT("EVENT", "赛事资讯"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String description;

    PublicAnnouncementCategoryEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PublicAnnouncementCategoryEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalized = code.trim();
        for (PublicAnnouncementCategoryEnum v : values()) {
            if (v.code.equalsIgnoreCase(normalized)) {
                return v;
            }
        }
        return null;
    }

    public static String resolveName(String code) {
        PublicAnnouncementCategoryEnum v = fromCode(code);
        return v == null ? code : v.getDescription();
    }
}
