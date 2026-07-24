package com.zhs.enums;

import com.zhs.exception.ServiceException;

/**
 * 球队排行维度，映射 basket_team_rank.rank_type。
 */
public enum PublicTeamRankTypeEnum {
    WIN_RATE("WIN_RATE", "胜率"),
    AVG_POINTS("AVG_POINTS", "场均得分"),
    AVG_POINTS_AGAINST("AVG_POINTS_AGAINST", "场均失分"),
    AVG_REBOUNDS("AVG_REBOUNDS", "场均篮板"),
    AVG_ASSISTS("AVG_ASSISTS", "场均助攻"),
    AVG_STEALS("AVG_STEALS", "场均抢断"),
    AVG_BLOCKS("AVG_BLOCKS", "场均盖帽"),
    FIELD_GOAL_PCT("FIELD_GOAL_PCT", "投篮命中率"),
    THREE_POINT_PCT("THREE_POINT_PCT", "三分命中率"),
    FREE_THROW_PCT("FREE_THROW_PCT", "罚球命中率");

    private final String code;
    private final String description;

    PublicTeamRankTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PublicTeamRankTypeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ServiceException("非法球队排行类型");
        }
        for (PublicTeamRankTypeEnum v : values()) {
            if (v.code.equalsIgnoreCase(code.trim())) {
                return v;
            }
        }
        throw new ServiceException("非法球队排行类型: " + code);
    }
}
