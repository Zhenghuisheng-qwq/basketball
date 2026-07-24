package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

/**
 * basket_home_banner.target_type 白名单。
 */
public enum BannerTargetTypeEnum {
    NONE("NONE"),
    TOURNAMENT("TOURNAMENT"),
    ANNOUNCEMENT("ANNOUNCEMENT"),
    TEAM("TEAM"),
    PLAYER("PLAYER"),
    EXTERNAL("EXTERNAL");

    private final String code;

    BannerTargetTypeEnum(String code) {
        this.code = code;
    }

    public String getCode() { return code; }

    public static BannerTargetTypeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return NONE;
        }
        for (BannerTargetTypeEnum v : values()) {
            if (v.code.equalsIgnoreCase(code.trim())) return v;
        }
        throw new ServiceException("非法跳转类型: " + code);
    }
}
