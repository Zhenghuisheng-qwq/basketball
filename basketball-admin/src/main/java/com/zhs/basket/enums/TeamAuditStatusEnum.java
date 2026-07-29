package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum TeamAuditStatusEnum {
    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已驳回"),
    REVIEWING(3, "审核中");

    private final Integer code;
    private final String description;

    TeamAuditStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() { return code; }
    public String getDescription() { return description; }

    /** 后台审核列表/详情统一展示文案 */
    public String getAuditDisplayDescription() {
        if (this == PENDING || this == REVIEWING) {
            return "待审核";
        }
        if (this == APPROVED) {
            return "已通过";
        }
        if (this == REJECTED) {
            return "已驳回";
        }
        return description;
    }

    public boolean isPendingAudit() {
        return this == PENDING || this == REVIEWING;
    }

    public static TeamAuditStatusEnum fromCode(Integer code) {
        if (code == null) throw new ServiceException("未知审核状态");
        for (TeamAuditStatusEnum v : values()) {
            if (v.code == code) return v;
        }
        throw new ServiceException("未知审核状态");
    }
}
