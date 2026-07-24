package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum MatchStatusEnum {
    DRAFT(0, "草稿"),
    SCHEDULED(1, "已编排"),
    RUNNING(2, "进行中"),
    FINISHED(3, "已结束"),
    POSTPONED(4, "已延期"),
    CANCELLED(5, "已取消");

    private final int code;
    private final String description;

    MatchStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public boolean canEdit() {
        return this == DRAFT || this == SCHEDULED || this == POSTPONED;
    }

    public boolean canSchedule() {
        return this == DRAFT || this == POSTPONED;
    }

    public boolean canStart() {
        return this == SCHEDULED;
    }

    public boolean canPostpone() {
        return this == SCHEDULED || this == RUNNING;
    }

    public boolean canCancel() {
        return this == DRAFT || this == SCHEDULED || this == POSTPONED;
    }

    public boolean canEnterLineup() {
        return this == SCHEDULED || this == RUNNING;
    }

    public boolean canEnterScore() {
        return this == RUNNING || this == FINISHED;
    }

    public boolean canEnterStat() {
        return this == RUNNING || this == FINISHED;
    }

    public boolean canDelete() {
        return this == DRAFT;
    }

    public static MatchStatusEnum fromCode(Integer code) {
        if (code == null) throw new ServiceException("未知比赛状态");
        for (MatchStatusEnum v : values()) {
            if (v.code == code) return v;
        }
        throw new ServiceException("未知比赛状态");
    }
}
