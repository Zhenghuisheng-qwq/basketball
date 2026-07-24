package com.zhs.basket.enums;

public enum TeamMemberRoleEnum {
    CAPTAIN("CAPTAIN"),
    MEMBER("MEMBER");

    private final String code;
    TeamMemberRoleEnum(String code) { this.code = code; }
    public String getCode() { return code; }
}
