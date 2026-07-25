package com.zhs.enums;

public enum RosterSourceTypeEnum {

    INITIAL_REGISTRATION("INITIAL_REGISTRATION", "初始报名"),
    OPEN_REGISTRATION("OPEN_REGISTRATION", "Open 报名"),
    TRANSFER("TRANSFER", "转会"),
    FREE_AGENT("FREE_AGENT", "自由球员补员");

    private final String code;
    private final String description;

    RosterSourceTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
