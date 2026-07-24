package com.zhs.basket.enums;

public enum SettlementTriggerTypeEnum {
    MANUAL_SEASON("MANUAL_SEASON", "手工重算赛季"),
    MANUAL_STAGE("MANUAL_STAGE", "手工重算阶段"),
    MANUAL_GROUP("MANUAL_GROUP", "手工重算小组"),
    MANUAL_MATCH("MANUAL_MATCH", "手工重算单场关联"),
    MATCH_CONFIRM("MATCH_CONFIRM", "比赛结果确认触发"),
    MATCH_REOPEN("MATCH_REOPEN", "比赛结果重开触发"),
    VALIDATION("VALIDATION", "数据校验");

    private final String code;
    private final String description;

    SettlementTriggerTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }
}
