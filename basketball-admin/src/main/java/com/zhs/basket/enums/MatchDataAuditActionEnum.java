package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum MatchDataAuditActionEnum {
    SUBMIT("SUBMIT", "提交审核"),
    RESUBMIT("RESUBMIT", "重新提交"),
    APPROVE("APPROVE", "审核通过"),
    REJECT("REJECT", "审核驳回"),
    AUTO_SYNC_OFFICIAL("AUTO_SYNC_OFFICIAL", "自动同步正式数据"),
    SYNC_FAILED("SYNC_FAILED", "同步失败");

    private final String code;
    private final String description;

    MatchDataAuditActionEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static MatchDataAuditActionEnum fromCode(String code) {
        if (code == null) {
            throw new ServiceException("未知审核动作");
        }
        for (MatchDataAuditActionEnum value : values()) {
            if (value.code.equalsIgnoreCase(code.trim())) {
                return value;
            }
        }
        throw new ServiceException("未知审核动作");
    }
}
