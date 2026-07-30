package com.zhs.basket.enums;

public enum ScheduleAuditActionTypeEnum {
    CREATE_MATCH("CREATE_MATCH"),
    GENERATE_DRAFT("GENERATE_DRAFT"),
    DELETE_DRAFT("DELETE_DRAFT"),
    PUBLISH("PUBLISH"),
    UNPUBLISH("UNPUBLISH"),
    UPDATE_MATCH("UPDATE_MATCH"),
    UPDATE_TEAMS("UPDATE_TEAMS"),
    UPDATE_TIME("UPDATE_TIME"),
    UPDATE_STATUS("UPDATE_STATUS"),
    SAVE_SCORE("SAVE_SCORE"),
    UPDATE_SCORE("UPDATE_SCORE");

    private final String code;

    ScheduleAuditActionTypeEnum(String code) {
        this.code = code;
    }

    public String getCode() { return code; }
}
