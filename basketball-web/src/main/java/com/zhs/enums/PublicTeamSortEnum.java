package com.zhs.enums;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketTeamSeasonStat;

/**
 * 公开球队赛季统计排序白名单（禁止任意 sortField 注入）。
 */
public enum PublicTeamSortEnum {
    WIN_RATE("winRate", BasketTeamSeasonStat::getWinRate),
    AVG_POINTS("avgPoints", BasketTeamSeasonStat::getAvgPoints),
    AVG_POINTS_AGAINST("avgPointsAgainst", BasketTeamSeasonStat::getAvgPointsAgainst),
    AVG_REBOUNDS("avgRebounds", BasketTeamSeasonStat::getAvgRebounds),
    AVG_ASSISTS("avgAssists", BasketTeamSeasonStat::getAvgAssists),
    AVG_STEALS("avgSteals", BasketTeamSeasonStat::getAvgSteals),
    AVG_BLOCKS("avgBlocks", BasketTeamSeasonStat::getAvgBlocks),
    FIELD_GOAL_PCT("fieldGoalPct", BasketTeamSeasonStat::getFieldGoalPct),
    THREE_POINT_PCT("threePointPct", BasketTeamSeasonStat::getThreePointPct),
    FREE_THROW_PCT("freeThrowPct", BasketTeamSeasonStat::getFreeThrowPct),
    GAMES("games", BasketTeamSeasonStat::getGames),
    WINS("wins", BasketTeamSeasonStat::getWins);

    private final String code;
    private final SFunction<BasketTeamSeasonStat, ?> column;

    PublicTeamSortEnum(String code, SFunction<BasketTeamSeasonStat, ?> column) {
        this.code = code;
        this.column = column;
    }

    public String getCode() {
        return code;
    }

    public SFunction<BasketTeamSeasonStat, ?> getColumn() {
        return column;
    }

    public static PublicTeamSortEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return WIN_RATE;
        }
        String normalized = code.trim();
        for (PublicTeamSortEnum v : values()) {
            if (v.name().equalsIgnoreCase(normalized) || v.code.equalsIgnoreCase(normalized)) {
                return v;
            }
        }
        throw new ServiceException("非法球队排序字段: " + code);
    }
}
