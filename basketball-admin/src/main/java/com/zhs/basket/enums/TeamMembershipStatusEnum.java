package com.zhs.basket.enums;

public enum TeamMembershipStatusEnum {
    INACTIVE(0), ACTIVE(1);
    private final int code;
    TeamMembershipStatusEnum(int code) { this.code = code; }
    public int getCode() { return code; }
}
