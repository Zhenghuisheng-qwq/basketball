package com.zhs.enums.v4;

public enum JoinStatusEnum {
    WAITING_TEAM_CREATION("WAITING_TEAM_CREATION", "等待建队"),
    WAITING_INVITE("WAITING_INVITE", "等待邀请"),
    INVITED("INVITED", "已邀请"),
    JOINED("JOINED", "已加入"),
    CONFIRMED("CONFIRMED", "已确认"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String description;

    JoinStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static JoinStatusEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (JoinStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知JoinStatusEnum: " + code);
    }
}
