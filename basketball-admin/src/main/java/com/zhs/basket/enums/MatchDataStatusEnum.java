package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum MatchDataStatusEnum {
    DRAFT(0, "草稿"),
    SUBMITTED(1, "已提交"),
    CONFIRMED(2, "已确认"),
    REOPENED(3, "已重开");

    private final int code;
    private final String description;

    MatchDataStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    /** 草稿/已提交/重开均可编辑；已确认锁定。提交后仍允许补充技术统计直至最终确认。 */
    public boolean canEdit() {
        return this == DRAFT || this == SUBMITTED || this == REOPENED;
    }

    public boolean canSubmit() {
        return this == DRAFT || this == REOPENED || this == SUBMITTED;
    }

    public boolean canConfirm() {
        return this == SUBMITTED;
    }

    public boolean canReopen() {
        return this == CONFIRMED;
    }

    public static MatchDataStatusEnum fromCode(Integer code) {
        if (code == null) throw new ServiceException("未知数据状态");
        for (MatchDataStatusEnum v : values()) {
            if (v.code == code) return v;
        }
        throw new ServiceException("未知数据状态");
    }
}
