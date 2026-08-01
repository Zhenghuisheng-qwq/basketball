package com.zhs.basket.enums;

import lombok.Getter;

@Getter
public enum TeamProfileChangeAuditStatusEnum {

    PENDING(1, "待审核"),
    REVIEWING(2, "审核中"),
    APPROVED(3, "已通过"),
    REJECTED(4, "已驳回"),
    CANCELLED(5, "已取消");

    private final int code;
    private final String label;

    TeamProfileChangeAuditStatusEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static TeamProfileChangeAuditStatusEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (TeamProfileChangeAuditStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }

    public static boolean isPending(Integer code) {
        return code != null && (code == PENDING.code || code == REVIEWING.code);
    }
}
