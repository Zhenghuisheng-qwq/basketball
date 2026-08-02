package com.zhs.enums;

import org.springframework.util.StringUtils;

public enum PublicPartnerSupportTypeEnum {
    CASH("CASH", "现金赞助"),
    GOODS("GOODS", "物资支持"),
    TECHNOLOGY("TECHNOLOGY", "技术支持"),
    VENUE("VENUE", "场地支持"),
    MEDIA("MEDIA", "媒体支持"),
    SERVICE("SERVICE", "服务支持"),
    OTHER("OTHER", "其他支持");

    private final String code;
    private final String description;

    PublicPartnerSupportTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PublicPartnerSupportTypeEnum fromCode(String code) {
        if (!StringUtils.hasText(code)) {
            return OTHER;
        }
        for (PublicPartnerSupportTypeEnum item : values()) {
            if (item.code.equalsIgnoreCase(code.trim())) {
                return item;
            }
        }
        return OTHER;
    }
}
