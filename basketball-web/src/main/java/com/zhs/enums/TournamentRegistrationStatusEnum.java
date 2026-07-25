package com.zhs.enums;

import com.zhs.exception.ServiceException;

public enum TournamentRegistrationStatusEnum {

    DRAFT(0, "草稿"),
    SUBMITTED(1, "已提交"),
    REVIEWING(2, "审核中"),
    APPROVED(3, "已通过"),
    REJECTED(4, "已驳回"),
    WITHDRAWN(5, "已撤回"),
    CANCELLED(6, "已取消");

    private final int code;
    private final String description;

    TournamentRegistrationStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public boolean canEdit() {
        return this == DRAFT || this == REJECTED;
    }

    public boolean canSubmit() {
        return this == DRAFT || this == REJECTED;
    }

    public boolean canWithdraw() {
        return this == REVIEWING || this == SUBMITTED;
    }

    /** 用户端展示文案（历史「已提交」按「审核中」展示） */
    public String getDisplayDescription() {
        if (this == SUBMITTED) {
            return REVIEWING.getDescription();
        }
        return description;
    }

    public boolean canDeleteDraft() {
        return this == DRAFT;
    }

    public static TournamentRegistrationStatusEnum fromCode(Integer code) {
        if (code == null) {
            throw new ServiceException("未知状态");
        }
        for (TournamentRegistrationStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new ServiceException("未知状态");
    }
}
