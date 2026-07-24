package com.zhs.basket.enums;

/**
 * 赛事规则类型。积分结算仅识别 STANDING_POINTS。
 */
public enum TournamentRuleTypeEnum {
    STANDING_POINTS("STANDING_POINTS", "积分与同分规则");

    private final String code;
    private final String description;

    TournamentRuleTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static boolean isStandingPoints(String ruleType) {
        return ruleType != null && STANDING_POINTS.code.equalsIgnoreCase(ruleType.trim());
    }
}
