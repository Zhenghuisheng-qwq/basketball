package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum TournamentRoundStatusEnum {
    DRAFT(0, "草稿"),
    SCHEDULED(1, "已编排"),
    ACTIVE(2, "进行中"),
    FINISHED(3, "已结束"),
    CANCELLED(4, "已取消");

    private final int code;
    private final String description;

    TournamentRoundStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static TournamentRoundStatusEnum fromCode(Integer code) {
        if (code == null) throw new ServiceException("未知轮次状态");
        for (TournamentRoundStatusEnum v : values()) {
            if (v.code == code) return v;
        }
        throw new ServiceException("未知轮次状态");
    }
}
