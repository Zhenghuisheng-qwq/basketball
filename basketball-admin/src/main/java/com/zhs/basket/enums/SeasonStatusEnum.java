package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum SeasonStatusEnum {
    PREPARE(0, "筹备中"),
    REGISTRATION_OPEN(1, "报名中"),
    IN_PROGRESS(2, "进行中"),
    FINISHED(3, "已结束"),
    REGISTRATION_CLOSED(4, "报名截止"),
    CLOSED(5, "已关闭"),
    CANCELLED(6, "已取消");

    private final int code;
    private final String description;

    SeasonStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static SeasonStatusEnum fromCode(Integer code) {
        if (code == null) throw new ServiceException("未知赛季状态");
        for (SeasonStatusEnum v : values()) {
            if (v.code == code) return v;
        }
        throw new ServiceException("未知赛季状态");
    }
}
