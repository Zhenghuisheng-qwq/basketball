package com.zhs.basket.support;

import com.zhs.basket.domain.BasketMatch;
import com.zhs.basket.enums.MatchDataStatusEnum;
import com.zhs.basket.enums.MatchStatusEnum;
import com.zhs.common.exception.ServiceException;

/**
 * 纳入结算的比赛权威判断（唯一入口，禁止各 Mapper 各写一套条件）。
 * <p>
 * 条件：未删除 + 已结束 + 数据已确认 + 未取消 + 主客队有效 + 比分有效。
 */
public final class ConfirmedMatchHelper {

    public static final int MATCH_STATUS_FINISHED = MatchStatusEnum.FINISHED.getCode();
    public static final int DATA_STATUS_CONFIRMED = MatchDataStatusEnum.CONFIRMED.getCode();
    public static final int MATCH_STATUS_CANCELLED = MatchStatusEnum.CANCELLED.getCode();
    public static final int NOT_DELETED = 0;

    private ConfirmedMatchHelper() {}

    public static boolean isConfirmedMatch(BasketMatch match) {
        if (match == null) {
            return false;
        }
        if (match.getIsDelete() != null && match.getIsDelete() != NOT_DELETED) {
            return false;
        }
        if (match.getMatchStatus() == null || match.getMatchStatus() != MATCH_STATUS_FINISHED) {
            return false;
        }
        if (match.getDataStatus() == null || match.getDataStatus() != DATA_STATUS_CONFIRMED) {
            return false;
        }
        if (match.getHomeTeamId() == null || match.getAwayTeamId() == null) {
            return false;
        }
        if (match.getHomeTeamId().equals(match.getAwayTeamId())) {
            return false;
        }
        if (match.getHomeScore() == null || match.getAwayScore() == null) {
            return false;
        }
        if (match.getHomeScore() < 0 || match.getAwayScore() < 0) {
            return false;
        }
        return true;
    }

    public static void requireConfirmedMatch(BasketMatch match) {
        if (!isConfirmedMatch(match)) {
            throw new ServiceException("比赛未最终确认，不能参与数据结算");
        }
    }

    /**
     * MyBatis SQL 片段条件说明（与 isConfirmedMatch 保持一致）：
     * is_delete = 0 AND match_status = 3 AND data_status = 2
     * AND home_team_id IS NOT NULL AND away_team_id IS NOT NULL
     * AND home_team_id != away_team_id
     * AND home_score >= 0 AND away_score >= 0
     */
    public static String sqlConfirmedPredicate(String alias) {
        String a = (alias == null || alias.isBlank()) ? "" : alias.trim() + ".";
        return a + "is_delete = 0"
                + " AND " + a + "match_status = " + MATCH_STATUS_FINISHED
                + " AND " + a + "data_status = " + DATA_STATUS_CONFIRMED
                + " AND " + a + "home_team_id IS NOT NULL"
                + " AND " + a + "away_team_id IS NOT NULL"
                + " AND " + a + "home_team_id <> " + a + "away_team_id"
                + " AND " + a + "home_score >= 0"
                + " AND " + a + "away_score >= 0";
    }
}
