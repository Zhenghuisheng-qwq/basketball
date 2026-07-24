package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum MatchPeriodTypeEnum {
    REGULAR("REGULAR", "常规节"),
    OVERTIME("OVERTIME", "加时");

    private final String code;
    private final String description;

    MatchPeriodTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static MatchPeriodTypeEnum fromCode(String code) {
        if (code == null || code.isBlank()) throw new ServiceException("未知节次类型");
        for (MatchPeriodTypeEnum v : values()) {
            if (v.code.equalsIgnoreCase(code.trim())) return v;
        }
        throw new ServiceException("未知节次类型");
    }
}
