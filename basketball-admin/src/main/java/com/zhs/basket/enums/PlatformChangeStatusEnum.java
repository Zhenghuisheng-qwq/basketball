package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum PlatformChangeStatusEnum {
    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已驳回"),
    WITHDRAWN(3, "已撤回");

    private final Integer code;
    private final String description;

    PlatformChangeStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /** 后台审核列表/详情统一展示文案 */
    public String getAuditDisplayDescription() {
        if (this == WITHDRAWN) {
            return "已取消";
        }
        return description;
    }

    public boolean isPendingAudit() {
        return this == PENDING;
    }

    public static PlatformChangeStatusEnum fromCode(Integer code) {
        if (code == null) {
            return PENDING;
        }
        for (PlatformChangeStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new ServiceException("未知平台变更状态");
    }
}
