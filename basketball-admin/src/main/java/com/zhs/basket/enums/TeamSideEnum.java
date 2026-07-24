package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum TeamSideEnum {
    HOME("HOME", "主队"),
    AWAY("AWAY", "客队");

    private final String code;
    private final String description;

    TeamSideEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static TeamSideEnum fromCode(String code) {
        if (code == null || code.isBlank()) throw new ServiceException("未知主客队类型");
        for (TeamSideEnum v : values()) {
            if (v.code.equalsIgnoreCase(code.trim())) return v;
        }
        throw new ServiceException("未知主客队类型");
    }
}
