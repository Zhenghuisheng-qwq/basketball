package com.zhs.enums;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketPlayerSeasonStat;

/**
 * 公开球员赛季统计排序白名单（禁止任意 sortField 注入）。
 */
public enum PublicPlayerSortEnum {
    AVG_POINTS("avgPoints", BasketPlayerSeasonStat::getAvgPoints),
    AVG_REBOUNDS("avgRebounds", BasketPlayerSeasonStat::getAvgRebounds),
    AVG_ASSISTS("avgAssists", BasketPlayerSeasonStat::getAvgAssists),
    AVG_STEALS("avgSteals", BasketPlayerSeasonStat::getAvgSteals),
    AVG_BLOCKS("avgBlocks", BasketPlayerSeasonStat::getAvgBlocks),
    FIELD_GOAL_PCT("fieldGoalPct", BasketPlayerSeasonStat::getFieldGoalPct),
    THREE_POINT_PCT("threePointPct", BasketPlayerSeasonStat::getThreePointPct),
    FREE_THROW_PCT("freeThrowPct", BasketPlayerSeasonStat::getFreeThrowPct),
    AVG_EFFICIENCY("avgEfficiency", BasketPlayerSeasonStat::getAvgEfficiency),
    GAMES("games", BasketPlayerSeasonStat::getGames),
    POINTS("points", BasketPlayerSeasonStat::getPoints);

    private final String code;
    private final SFunction<BasketPlayerSeasonStat, ?> column;

    PublicPlayerSortEnum(String code, SFunction<BasketPlayerSeasonStat, ?> column) {
        this.code = code;
        this.column = column;
    }

    public String getCode() {
        return code;
    }

    public SFunction<BasketPlayerSeasonStat, ?> getColumn() {
        return column;
    }

    public static PublicPlayerSortEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return AVG_POINTS;
        }
        String normalized = code.trim();
        for (PublicPlayerSortEnum v : values()) {
            if (v.name().equalsIgnoreCase(normalized) || v.code.equalsIgnoreCase(normalized)) {
                return v;
            }
        }
        throw new ServiceException("非法球员排序字段: " + code);
    }
}
