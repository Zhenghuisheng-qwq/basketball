package com.zhs.enums;

import com.zhs.exception.ServiceException;

/**
 * 球员认证申请状态（application_status / certification_status）
 */
public enum PlayerCertStatusEnum {

    DRAFT(0, "草稿"),
    SUBMITTED(1, "已提交"),
    REVIEWING(2, "审核中"),
    APPROVED(3, "已通过"),
    REJECTED(4, "已驳回"),
    CANCELLED(5, "已取消");

    private final int code;
    private final String description;

    PlayerCertStatusEnum(int code, String description) {
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

    public boolean canDeleteMaterial() {
        return canEdit();
    }

    public boolean isApproved() {
        return this == APPROVED;
    }

    public boolean isReviewing() {
        return this == SUBMITTED || this == REVIEWING;
    }

    public static PlayerCertStatusEnum fromCode(Integer code) {
        if (code == null) {
            return DRAFT;
        }
        for (PlayerCertStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new ServiceException("未知认证状态");
    }
}
