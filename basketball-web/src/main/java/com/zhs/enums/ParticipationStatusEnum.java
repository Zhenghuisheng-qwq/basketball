package com.zhs.enums;

import com.zhs.exception.ServiceException;

public enum ParticipationStatusEnum {

    ACTIVE(1, "参赛中"),
    WITHDRAWN(2, "已退赛"),
    DISQUALIFIED(3, "取消资格");

    private final int code;
    private final String description;

    ParticipationStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ParticipationStatusEnum fromCode(Integer code) {
        if (code == null) {
            return ACTIVE;
        }
        for (ParticipationStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new ServiceException("未知参赛状态");
    }
}
