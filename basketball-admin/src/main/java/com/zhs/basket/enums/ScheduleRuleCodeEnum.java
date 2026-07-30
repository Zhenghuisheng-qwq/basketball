package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum ScheduleRuleCodeEnum {
    REGULAR_ROUND_ROBIN("REGULAR_ROUND_ROBIN", "常规赛单循环");

    private final String code;
    private final String description;

    ScheduleRuleCodeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static ScheduleRuleCodeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ServiceException("赛程规则编码不能为空");
        }
        for (ScheduleRuleCodeEnum v : values()) {
            if (v.code.equalsIgnoreCase(code.trim())) {
                return v;
            }
        }
        throw new ServiceException("未知赛程规则编码: " + code);
    }
}
