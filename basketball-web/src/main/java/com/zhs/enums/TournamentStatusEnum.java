package com.zhs.enums;

import com.zhs.exception.ServiceException;

public enum TournamentStatusEnum {

    DISABLED(0, "已禁用"),
    ACTIVE(1, "进行中"),
    ENDED(2, "已结束"),
    DRAFT(3, "草稿");

    private final int code;
    private final String description;

    TournamentStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }


    public static TournamentStatusEnum fromCode(Integer code) {
        if (code == null) {
            throw new ServiceException("未知状态");
        }
        for (TournamentStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new ServiceException("未知状态");
    }
}
