package com.zhs.basket.enums;

import lombok.Getter;

@Getter
public enum MemberProfileChangeAuditStatusEnum {

    PENDING(1, "待审核"),
    APPROVED(3, "已通过"),
    REJECTED(4, "已驳回"),
    CANCELLED(5, "已取消");

    private final int code;
    private final String label;

    MemberProfileChangeAuditStatusEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static MemberProfileChangeAuditStatusEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (MemberProfileChangeAuditStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }

    public static boolean isPending(Integer code) {
        return code != null && code == PENDING.code;
    }
}
