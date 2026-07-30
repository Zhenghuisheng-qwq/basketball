package com.zhs.enums;

import com.zhs.exception.ServiceException;

public enum MatchPeriodTypeEnum {

    REGULAR("REGULAR", "常规节"),
    OVERTIME("OVERTIME", "加时");

    private final String code;
    private final String description;

    MatchPeriodTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static MatchPeriodTypeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ServiceException("未知节次类型");
        }
        for (MatchPeriodTypeEnum value : values()) {
            if (value.code.equalsIgnoreCase(code.trim())) {
                return value;
            }
        }
        throw new ServiceException("未知节次类型");
    }
}
