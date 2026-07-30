package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum MatchDataAuditActionEnum {
    SUBMIT("SUBMIT"),
    RESUBMIT("RESUBMIT"),
    APPROVE("APPROVE"),
    REJECT("REJECT"),
    AUTO_SYNC_OFFICIAL("AUTO_SYNC_OFFICIAL"),
    SYNC_FAILED("SYNC_FAILED");

    private final String code;

    MatchDataAuditActionEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
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
