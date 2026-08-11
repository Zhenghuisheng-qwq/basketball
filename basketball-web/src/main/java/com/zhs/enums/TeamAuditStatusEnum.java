package com.zhs.enums;

import com.zhs.exception.ServiceException;

public enum TeamAuditStatusEnum {

    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已驳回"),
    REVIEWING(3, "审核中"),
    /** 已创建未提交认证；tinyint 可表达，无需 DDL */
    DRAFT(4, "草稿");

    private final int code;
    private final String description;

    TeamAuditStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /** 认证审核进行中（禁止改认证资料） */
    public boolean isPendingAudit() {
        return this == PENDING || this == REVIEWING;
    }

    /** 是否已完成首次认证 */
    public boolean isApproved() {
        return this == APPROVED;
    }

    /** 允许直接编辑待认证球队资料（草稿 / 驳回后修改） */
    public boolean allowsDirectProfileEdit() {
        return this == DRAFT || this == REJECTED;
    }

    /** 允许提交 / 重新提交球队认证 */
    public boolean canSubmitCertification() {
        return this == DRAFT || this == REJECTED;
    }

    public static TeamAuditStatusEnum fromCode(Integer code) {
        if (code == null) {
            throw new ServiceException("未知状态");
        }
        for (TeamAuditStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new ServiceException("未知状态");
    }
}
