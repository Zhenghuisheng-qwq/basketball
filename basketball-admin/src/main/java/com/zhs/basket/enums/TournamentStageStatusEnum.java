package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum TournamentStageStatusEnum {
    DRAFT(0, "草稿"),
    ACTIVE(1, "进行中"),
    FINISHED(2, "已结束"),
    CANCELLED(3, "已取消");

    private final int code;
    private final String description;

    TournamentStageStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static TournamentStageStatusEnum fromCode(Integer code) {
        if (code == null) throw new ServiceException("未知阶段状态");
        for (TournamentStageStatusEnum v : values()) {
            if (v.code == code) return v;
        }
        throw new ServiceException("未知阶段状态");
    }
}
