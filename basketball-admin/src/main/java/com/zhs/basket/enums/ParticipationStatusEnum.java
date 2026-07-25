package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum ParticipationStatusEnum {
    ACTIVE(1, "参赛中"),
    WITHDRAWN(2, "已退赛"),
    DISQUALIFIED(3, "取消资格"),
    FINISHED(4, "已完赛");

    private final int code;
    private final String description;

    ParticipationStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static ParticipationStatusEnum fromCode(Integer code) {
        if (code == null) throw new ServiceException("未知参赛状态");
        for (ParticipationStatusEnum v : values()) {
            if (v.code == code) return v;
        }
        throw new ServiceException("未知参赛状态");
    }
}
