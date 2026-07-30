package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum ScheduleRuleVersionEnum {
    V1("V1"),
    V2("V2");

    private final String code;

    ScheduleRuleVersionEnum(String code) {
        this.code = code;
    }

    public String getCode() { return code; }

    public static ScheduleRuleVersionEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ServiceException("赛程规则版本不能为空");
        }
        for (ScheduleRuleVersionEnum v : values()) {
            if (v.code.equalsIgnoreCase(code.trim())) {
                return v;
            }
        }
        throw new ServiceException("未知赛程规则版本: " + code);
    }
}
