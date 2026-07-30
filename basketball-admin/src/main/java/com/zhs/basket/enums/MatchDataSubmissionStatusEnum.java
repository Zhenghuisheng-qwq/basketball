package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum MatchDataSubmissionStatusEnum {
    DRAFT(0, "草稿"),
    SUBMITTED(1, "审核中"),
    APPROVED(2, "审核通过"),
    REJECTED(3, "审核驳回"),
    SUPERSEDED(4, "已被新版本替代"),
    CANCELLED(5, "已取消");

    private final int code;
    private final String description;

    MatchDataSubmissionStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPendingAudit() {
        return this == SUBMITTED;
    }

    public static MatchDataSubmissionStatusEnum fromCode(Integer code) {
        if (code == null) {
            throw new ServiceException("未知提交状态");
        }
        for (MatchDataSubmissionStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new ServiceException("未知提交状态");
    }
}
