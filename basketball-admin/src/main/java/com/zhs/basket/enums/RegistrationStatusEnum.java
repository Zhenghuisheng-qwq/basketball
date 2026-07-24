package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum RegistrationStatusEnum {
    DRAFT(0, "草稿"),
    SUBMITTED(1, "已提交"),
    REVIEWING(2, "审核中"),
    APPROVED(3, "已通过"),
    REJECTED(4, "已驳回"),
    WITHDRAWN(5, "已撤回"),
    CANCELLED(6, "已取消");

    private final int code;
    private final String description;

    RegistrationStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static RegistrationStatusEnum fromCode(Integer code) {
        if (code == null) throw new ServiceException("未知报名状态");
        for (RegistrationStatusEnum v : values()) {
            if (v.code == code) return v;
        }
        throw new ServiceException("未知报名状态");
    }
}
