package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

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

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static PlayerCertStatusEnum fromCode(Integer code) {
        if (code == null) return DRAFT;
        for (PlayerCertStatusEnum v : values()) {
            if (v.code == code) return v;
        }
        throw new ServiceException("未知认证状态");
    }
}
