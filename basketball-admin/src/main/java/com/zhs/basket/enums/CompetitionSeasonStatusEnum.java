package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum CompetitionSeasonStatusEnum {
    PREPARE(0, "筹备中"),
    ACTIVE(1, "进行中"),
    FINISHED(2, "已结束"),
    ARCHIVED(3, "已归档");

    private final int code;
    private final String description;

    CompetitionSeasonStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }

    public String getDescription() { return description; }

    public static CompetitionSeasonStatusEnum fromCode(Integer code) {
        if (code == null) {
            throw new ServiceException("未知赛季状态");
        }
        for (CompetitionSeasonStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new ServiceException("未知赛季状态");
    }
}
