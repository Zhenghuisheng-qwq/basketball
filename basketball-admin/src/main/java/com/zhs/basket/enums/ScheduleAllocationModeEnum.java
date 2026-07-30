package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

/**
 * 单轮比赛在时间槽之间的分配策略。
 */
public enum ScheduleAllocationModeEnum {
    FILL_FIRST("FILL_FIRST", "优先填满靠前时间槽"),
    EVEN_SPLIT("EVEN_SPLIT", "尽量均分到各时间槽");

    private final String code;
    private final String description;

    ScheduleAllocationModeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ScheduleAllocationModeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return FILL_FIRST;
        }
        for (ScheduleAllocationModeEnum value : values()) {
            if (value.code.equalsIgnoreCase(code.trim())) {
                return value;
            }
        }
        throw new ServiceException("未知时间槽分配策略: " + code);
    }
}
