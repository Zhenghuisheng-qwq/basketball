package com.zhs.support.v4;

import com.zhs.response.v4.publicapi.V4PublicMatchPlayerStatItemResp;
import com.zhs.response.v4.publicapi.V4PublicMatchSeriesGameResp;
import com.zhs.response.v4.publicapi.V4PublicMatchTeamStatItemResp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Public MatchSeries aggregate: SUM official TeamStat / PlayerStat only.
 * Do not recompute PTS. Do not average shooting percentages.
 */
public final class V4PublicMatchSeriesStatAggregator {

    private V4PublicMatchSeriesStatAggregator() {
    }

    public static Set<Long> eligibleMatchIds(List<V4PublicMatchSeriesGameResp> games, Set<Long> matchIdsWithOfficialStats) {
        Set<Long> result = new HashSet<>();
        if (games == null || matchIdsWithOfficialStats == null || matchIdsWithOfficialStats.isEmpty()) {
            return result;
        }
        for (V4PublicMatchSeriesGameResp game : games) {
            if (game == null || game.getMatchId() == null) {
                continue;
            }
            if (Boolean.TRUE.equals(game.getSkipped())) {
                continue;
            }
            if (matchIdsWithOfficialStats.contains(game.getMatchId())) {
                result.add(game.getMatchId());
            }
        }
        return result;
    }

    public static void markStatsAvailable(List<V4PublicMatchSeriesGameResp> games, Set<Long> matchIdsWithOfficialStats) {
        if (games == null) {
            return;
        }
        Set<Long> stats = matchIdsWithOfficialStats == null ? Set.of() : matchIdsWithOfficialStats;
        for (V4PublicMatchSeriesGameResp game : games) {
            if (game == null) {
                continue;
            }
            game.setStatsAvailable(game.getMatchId() != null
                    && !Boolean.TRUE.equals(game.getSkipped())
                    && stats.contains(game.getMatchId()));
        }
    }

    public static List<V4PublicMatchTeamStatItemResp> sumTeamStats(List<V4PublicMatchTeamStatItemResp> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        Map<Long, V4PublicMatchTeamStatItemResp> byTeam = new HashMap<>();
        for (V4PublicMatchTeamStatItemResp row : rows) {
            if (row == null || row.getEventTeamId() == null) {
                continue;
            }
            V4PublicMatchTeamStatItemResp acc = byTeam.computeIfAbsent(row.getEventTeamId(), id -> {
                V4PublicMatchTeamStatItemResp vo = new V4PublicMatchTeamStatItemResp();
                vo.setEventTeamId(id);
                vo.setTeamName(row.getTeamName());
                return vo;
            });
            if (acc.getTeamName() == null || acc.getTeamName().isBlank()) {
                acc.setTeamName(row.getTeamName());
            }
            acc.setScore(sum(acc.getScore(), row.getScore()));
            acc.setPointsFor(sum(acc.getPointsFor(), row.getPointsFor()));
            acc.setPointsAgainst(sum(acc.getPointsAgainst(), row.getPointsAgainst()));
            acc.setFieldGoalMade(sum(acc.getFieldGoalMade(), row.getFieldGoalMade()));
            acc.setFieldGoalAttempt(sum(acc.getFieldGoalAttempt(), row.getFieldGoalAttempt()));
            acc.setThreePointMade(sum(acc.getThreePointMade(), row.getThreePointMade()));
            acc.setThreePointAttempt(sum(acc.getThreePointAttempt(), row.getThreePointAttempt()));
            acc.setFreeThrowMade(sum(acc.getFreeThrowMade(), row.getFreeThrowMade()));
            acc.setFreeThrowAttempt(sum(acc.getFreeThrowAttempt(), row.getFreeThrowAttempt()));
            acc.setRebound(sum(acc.getRebound(), row.getRebound()));
            acc.setAssist(sum(acc.getAssist(), row.getAssist()));
            acc.setSteal(sum(acc.getSteal(), row.getSteal()));
            acc.setBlock(sum(acc.getBlock(), row.getBlock()));
            acc.setTurnover(sum(acc.getTurnover(), row.getTurnover()));
            acc.setFoul(sum(acc.getFoul(), row.getFoul()));
        }
        return new ArrayList<>(byTeam.values());
    }

    public static List<V4PublicMatchPlayerStatItemResp> sumPlayerStats(List<V4PublicMatchPlayerStatItemResp> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        Map<Long, V4PublicMatchPlayerStatItemResp> byPlayer = new HashMap<>();
        for (V4PublicMatchPlayerStatItemResp row : rows) {
            if (row == null || row.getEventPlayerId() == null) {
                continue;
            }
            V4PublicMatchPlayerStatItemResp acc = byPlayer.computeIfAbsent(row.getEventPlayerId(), id -> {
                V4PublicMatchPlayerStatItemResp vo = new V4PublicMatchPlayerStatItemResp();
                vo.setEventPlayerId(id);
                vo.setSourcePlayerId(row.getSourcePlayerId());
                vo.setPlayerName(row.getPlayerName());
                vo.setEventTeamId(row.getEventTeamId());
                vo.setTeamName(row.getTeamName());
                return vo;
            });
            if (acc.getPlayerName() == null || acc.getPlayerName().isBlank()) {
                acc.setPlayerName(row.getPlayerName());
            }
            if (acc.getSourcePlayerId() == null) {
                acc.setSourcePlayerId(row.getSourcePlayerId());
            }
            if (acc.getEventTeamId() == null) {
                acc.setEventTeamId(row.getEventTeamId());
            }
            if (acc.getTeamName() == null || acc.getTeamName().isBlank()) {
                acc.setTeamName(row.getTeamName());
            }
            acc.setPoints(sum(acc.getPoints(), row.getPoints()));
            acc.setRebound(sum(acc.getRebound(), row.getRebound()));
            acc.setAssist(sum(acc.getAssist(), row.getAssist()));
            acc.setSteal(sum(acc.getSteal(), row.getSteal()));
            acc.setBlock(sum(acc.getBlock(), row.getBlock()));
            acc.setTurnover(sum(acc.getTurnover(), row.getTurnover()));
            acc.setFoul(sum(acc.getFoul(), row.getFoul()));
            acc.setFieldGoalMade(sum(acc.getFieldGoalMade(), row.getFieldGoalMade()));
            acc.setFieldGoalAttempt(sum(acc.getFieldGoalAttempt(), row.getFieldGoalAttempt()));
            acc.setThreePointMade(sum(acc.getThreePointMade(), row.getThreePointMade()));
            acc.setThreePointAttempt(sum(acc.getThreePointAttempt(), row.getThreePointAttempt()));
            acc.setFreeThrowMade(sum(acc.getFreeThrowMade(), row.getFreeThrowMade()));
            acc.setFreeThrowAttempt(sum(acc.getFreeThrowAttempt(), row.getFreeThrowAttempt()));
        }
        List<V4PublicMatchPlayerStatItemResp> result = new ArrayList<>(byPlayer.values());
        result.sort(Comparator.comparing(V4PublicMatchPlayerStatItemResp::getPoints,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    static Integer sum(Integer left, Integer right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left + right;
    }
}
