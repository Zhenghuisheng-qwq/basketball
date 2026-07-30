package com.zhs.enums;

import com.zhs.exception.ServiceException;

/**
 * 比赛数据提交主表 submission_status。
 */
public enum MatchDataSubmissionStatusEnum {

    DRAFT(0, "草稿"),
    PENDING(1, "审核中"),
    APPROVED(2, "已通过"),
    REJECTED(3, "已驳回"),
    SUPERSEDED(4, "被替代"),
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

    public boolean isActive() {
        return this == PENDING || this == APPROVED;
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
