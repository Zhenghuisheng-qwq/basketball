package com.zhs.basket.enums.v4;

public enum EventTeamStatusEnum {
    ACTIVE("ACTIVE", "有效"),
    INACTIVE("INACTIVE", "无效"),
    DISQUALIFIED("DISQUALIFIED", "赛事资格已取消");

    private final String code;
    private final String description;

    EventTeamStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static boolean isDisqualified(String status) {
        return DISQUALIFIED.code.equalsIgnoreCase(status);
    }

    public static boolean isStandingEligible(String status) {
        return ACTIVE.code.equalsIgnoreCase(status) || DISQUALIFIED.code.equalsIgnoreCase(status);
    }
}