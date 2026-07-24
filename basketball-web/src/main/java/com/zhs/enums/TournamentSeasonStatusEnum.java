package com.zhs.enums;

import com.zhs.exception.ServiceException;

public enum TournamentSeasonStatusEnum {

    PREPARE(0, "筹备中"),
    REGISTRATION_OPEN(1, "报名中"),
    IN_PROGRESS(2, "进行中"),
    FINISHED(3, "已结束"),
    REGISTRATION_CLOSED(4, "报名截止"),
    CLOSED(5, "已关闭"),
    CANCELLED(6, "已取消");

    private final int code;
    private final String description;

    TournamentSeasonStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }


    public static TournamentSeasonStatusEnum fromCode(Integer code) {
        if (code == null) {
            throw new ServiceException("未知状态");
        }
        for (TournamentSeasonStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new ServiceException("未知状态");
    }
}
