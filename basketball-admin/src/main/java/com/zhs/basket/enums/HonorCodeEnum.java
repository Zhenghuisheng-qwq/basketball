package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

/**
 * basket_honor_category.honor_code 约定编码。
 */
public enum HonorCodeEnum {
    CHAMPION("CHAMPION", "冠军", "TOURNAMENT", "TEAM"),
    RUNNER_UP("RUNNER_UP", "亚军", "TOURNAMENT", "TEAM"),
    THIRD_PLACE("THIRD_PLACE", "季军", "TOURNAMENT", "TEAM"),
    SCORING_CHAMPION("SCORING_CHAMPION", "得分王", "PLAYER", "PLAYER"),
    REBOUND_CHAMPION("REBOUND_CHAMPION", "篮板王", "PLAYER", "PLAYER"),
    ASSIST_CHAMPION("ASSIST_CHAMPION", "助攻王", "PLAYER", "PLAYER"),
    STEAL_CHAMPION("STEAL_CHAMPION", "抢断王", "PLAYER", "PLAYER"),
    BLOCK_CHAMPION("BLOCK_CHAMPION", "盖帽王", "PLAYER", "PLAYER"),
    TEAM_HONOR("TEAM_HONOR", "球队荣誉", "TEAM", "TEAM"),
    PLAYER_HONOR("PLAYER_HONOR", "球员荣誉", "PLAYER", "PLAYER");

    private final String code;
    private final String name;
    private final String scope;
    private final String recipientType;

    HonorCodeEnum(String code, String name, String scope, String recipientType) {
        this.code = code;
        this.name = name;
        this.scope = scope;
        this.recipientType = recipientType;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getScope() { return scope; }
    public String getRecipientType() { return recipientType; }

    public static HonorCodeEnum fromCode(String code) {
        if (code == null || code.isBlank()) throw new ServiceException("未知荣誉编码");
        for (HonorCodeEnum v : values()) {
            if (v.code.equalsIgnoreCase(code.trim())) return v;
        }
        throw new ServiceException("未知荣誉编码: " + code);
    }
}
