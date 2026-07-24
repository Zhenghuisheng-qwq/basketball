package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

/**
 * 球员排行维度，映射 basket_player_rank.rank_type。
 * 效率榜仅在赛季统计已汇总 avg_efficiency（来源单场已录入 efficiency）时开放。
 */
public enum PlayerRankTypeEnum {
    AVG_POINTS("AVG_POINTS", "场均得分"),
    AVG_REBOUNDS("AVG_REBOUNDS", "场均篮板"),
    AVG_ASSISTS("AVG_ASSISTS", "场均助攻"),
    AVG_STEALS("AVG_STEALS", "场均抢断"),
    AVG_BLOCKS("AVG_BLOCKS", "场均盖帽"),
    FIELD_GOAL_PCT("FIELD_GOAL_PCT", "投篮命中率"),
    THREE_POINT_PCT("THREE_POINT_PCT", "三分命中率"),
    FREE_THROW_PCT("FREE_THROW_PCT", "罚球命中率"),
    AVG_EFFICIENCY("AVG_EFFICIENCY", "场均效率值");

    private final String code;
    private final String description;

    PlayerRankTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static PlayerRankTypeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ServiceException("非法球员排行类型");
        }
        for (PlayerRankTypeEnum v : values()) {
            if (v.code.equalsIgnoreCase(code.trim())) {
                return v;
            }
        }
        throw new ServiceException("非法球员排行类型: " + code);
    }
}
