package com.zhs.basket.enums.v4;

/**
 * CNPA 赛事域 V4 枚举：RankTypeEnum
 * 存储方式：VARCHAR 字符串 code，与旧整型枚举隔离。
 */
public enum RankTypeEnum {
    TEAM_STANDING("TEAM_STANDING", "球队积分榜"),
    TEAM_SCORE("TEAM_SCORE", "球队得分榜"),
    PLAYER_SCORE("PLAYER_SCORE", "球员得分榜"),
    PLAYER_ASSIST("PLAYER_ASSIST", "球员助攻榜"),
    PLAYER_REBOUND("PLAYER_REBOUND", "球员篮板榜"),
    PLAYER_STEAL("PLAYER_STEAL", "球员抢断榜"),
    PLAYER_BLOCK("PLAYER_BLOCK", "球员盖帽榜");

    private final String code;
    private final String description;

    RankTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static RankTypeEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (RankTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知RankTypeEnum: " + code);
    }
}
