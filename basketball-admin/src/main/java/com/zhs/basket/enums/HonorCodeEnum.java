package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

/**
 * basket_honor_category.honor_code 约定编码。
 */
public enum HonorCodeEnum {
    CHAMPION("CHAMPION", "冠军", "TOURNAMENT", "TEAM", "赛季冠军", 10),
    RUNNER_UP("RUNNER_UP", "亚军", "TOURNAMENT", "TEAM", "赛季亚军", 20),
    THIRD_PLACE("THIRD_PLACE", "季军", "TOURNAMENT", "TEAM", "赛季季军", 30),
    TEAM_HONOR("TEAM_HONOR", "球队荣誉", "TEAM", "TEAM", "人工球队荣誉", 40),
    SCORING_CHAMPION("SCORING_CHAMPION", "得分王", "PLAYER", "PLAYER", "赛季得分王", 50),
    REBOUND_CHAMPION("REBOUND_CHAMPION", "篮板王", "PLAYER", "PLAYER", "赛季篮板王", 60),
    ASSIST_CHAMPION("ASSIST_CHAMPION", "助攻王", "PLAYER", "PLAYER", "赛季助攻王", 70),
    STEAL_CHAMPION("STEAL_CHAMPION", "抢断王", "PLAYER", "PLAYER", "赛季抢断王", 80),
    BLOCK_CHAMPION("BLOCK_CHAMPION", "盖帽王", "PLAYER", "PLAYER", "赛季盖帽王", 90),
    PLAYER_HONOR("PLAYER_HONOR", "球员荣誉", "PLAYER", "PLAYER", "人工球员荣誉", 100),
    WEEKLY_BEST("WEEKLY_BEST", "周最佳", "PLAYER", "PLAYER", "人工评选周最佳", 110),
    MVP("MVP", "赛事MVP", "TOURNAMENT", "PLAYER", "人工评选赛事MVP", 120),
    ALL_TOURNAMENT("ALL_TOURNAMENT", "最佳阵容", "PLAYER", "PLAYER", "人工评选最佳阵容", 130),
    ALL_DEFENSIVE("ALL_DEFENSIVE", "最佳防守阵容", "PLAYER", "PLAYER", "人工评选最佳防守阵容", 140);

    private final String code;
    private final String name;
    private final String scope;
    private final String recipientType;
    private final String description;
    private final int displayOrder;

    HonorCodeEnum(String code, String name, String scope, String recipientType, String description, int displayOrder) {
        this.code = code;
        this.name = name;
        this.scope = scope;
        this.recipientType = recipientType;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getScope() { return scope; }
    public String getRecipientType() { return recipientType; }
    public String getDescription() { return description; }
    public int getDisplayOrder() { return displayOrder; }

    public static HonorCodeEnum find(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (HonorCodeEnum v : values()) {
            if (v.code.equalsIgnoreCase(code.trim())) {
                return v;
            }
        }
        return null;
    }

    public static HonorCodeEnum fromCode(String code) {
        HonorCodeEnum found = find(code);
        if (found == null) {
            throw new ServiceException(code == null || code.isBlank() ? "未知荣誉编码" : "未知荣誉编码: " + code);
        }
        return found;
    }
}
