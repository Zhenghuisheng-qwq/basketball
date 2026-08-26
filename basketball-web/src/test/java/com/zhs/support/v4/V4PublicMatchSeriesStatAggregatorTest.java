package com.zhs.support.v4;

import com.zhs.response.v4.publicapi.V4PublicMatchPlayerStatItemResp;
import com.zhs.response.v4.publicapi.V4PublicMatchSeriesGameResp;
import com.zhs.response.v4.publicapi.V4PublicMatchTeamStatItemResp;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class V4PublicMatchSeriesStatAggregatorTest {

    @Test
    void bo3_playerPointsAreSumNotAverage() {
        List<V4PublicMatchPlayerStatItemResp> rows = List.of(
                player(1L, 10L, 16, 6, 10, 2, 5, 1, 2),
                player(1L, 10L, 18, 7, 12, 3, 6, 2, 2),
                player(1L, 10L, 14, 5, 9, 1, 4, 0, 1)
        );
        List<V4PublicMatchPlayerStatItemResp> total = V4PublicMatchSeriesStatAggregator.sumPlayerStats(rows);
        assertEquals(1, total.size());
        assertEquals(48, total.get(0).getPoints());
        assertEquals(18, total.get(0).getFieldGoalMade());
        assertEquals(31, total.get(0).getFieldGoalAttempt());
        assertEquals(6, total.get(0).getThreePointMade());
        assertEquals(15, total.get(0).getThreePointAttempt());
        assertEquals(3, total.get(0).getFreeThrowMade());
        assertEquals(5, total.get(0).getFreeThrowAttempt());
    }

    @Test
    void bo5_earlyComplete_skipsRemainingUnplayed() {
        List<V4PublicMatchSeriesGameResp> games = List.of(
                game(1L, 1, false),
                game(2L, 2, false),
                game(3L, 3, false),
                game(4L, 4, false),
                game(5L, 5, true)
        );
        Set<Long> withStats = Set.of(1L, 2L, 3L, 4L);
        Set<Long> eligible = V4PublicMatchSeriesStatAggregator.eligibleMatchIds(games, withStats);
        assertEquals(Set.of(1L, 2L, 3L, 4L), eligible);
        assertFalse(eligible.contains(5L));

        V4PublicMatchSeriesStatAggregator.markStatsAvailable(games, withStats);
        assertTrue(games.get(0).getStatsAvailable());
        assertFalse(games.get(4).getStatsAvailable());
    }

    @Test
    void bo7_earlyComplete_hidesUnplayedG7() {
        List<V4PublicMatchSeriesGameResp> games = List.of(
                game(11L, 1, false),
                game(12L, 2, false),
                game(13L, 3, false),
                game(14L, 4, false),
                game(15L, 5, false),
                game(16L, 6, false),
                game(17L, 7, true)
        );
        Set<Long> eligible = V4PublicMatchSeriesStatAggregator.eligibleMatchIds(games, Set.of(11L, 12L, 13L, 14L, 15L, 16L));
        assertEquals(6, eligible.size());
        assertFalse(eligible.contains(17L));
    }

    @Test
    void zeroScore_isKept() {
        V4PublicMatchTeamStatItemResp g1 = team(20L, 20, 0);
        V4PublicMatchTeamStatItemResp g2 = team(20L, 0, 18);
        List<V4PublicMatchTeamStatItemResp> total = V4PublicMatchSeriesStatAggregator.sumTeamStats(List.of(g1, g2));
        assertEquals(1, total.size());
        assertEquals(20, total.get(0).getScore());
        assertEquals(18, total.get(0).getPointsAgainst());
    }

    @Test
    void forfeitWithoutOfficialStats_isExcluded() {
        List<V4PublicMatchSeriesGameResp> games = List.of(
                game(1L, 1, false),
                game(2L, 2, false)
        );
        Set<Long> eligible = V4PublicMatchSeriesStatAggregator.eligibleMatchIds(games, Set.of(1L));
        assertEquals(Set.of(1L), eligible);
        V4PublicMatchSeriesStatAggregator.markStatsAvailable(games, Set.of(1L));
        assertTrue(games.get(0).getStatsAvailable());
        assertFalse(games.get(1).getStatsAvailable());
    }

    @Test
    void skippedGame_neverEligibleEvenIfStatsExist() {
        List<V4PublicMatchSeriesGameResp> games = List.of(game(9L, 5, true));
        Set<Long> eligible = V4PublicMatchSeriesStatAggregator.eligibleMatchIds(games, Set.of(9L));
        assertTrue(eligible.isEmpty());
    }

    private static V4PublicMatchSeriesGameResp game(Long matchId, int gameNo, boolean skipped) {
        V4PublicMatchSeriesGameResp game = new V4PublicMatchSeriesGameResp();
        game.setMatchId(matchId);
        game.setSeriesGameNo(gameNo);
        game.setSkipped(skipped);
        return game;
    }

    private static V4PublicMatchPlayerStatItemResp player(Long eventPlayerId, Long teamId, int points,
                                                          int fgm, int fga, int tpm, int tpa, int ftm, int fta) {
        V4PublicMatchPlayerStatItemResp vo = new V4PublicMatchPlayerStatItemResp();
        vo.setEventPlayerId(eventPlayerId);
        vo.setEventTeamId(teamId);
        vo.setPlayerName("Player A");
        vo.setPoints(points);
        vo.setFieldGoalMade(fgm);
        vo.setFieldGoalAttempt(fga);
        vo.setThreePointMade(tpm);
        vo.setThreePointAttempt(tpa);
        vo.setFreeThrowMade(ftm);
        vo.setFreeThrowAttempt(fta);
        return vo;
    }

    private static V4PublicMatchTeamStatItemResp team(Long eventTeamId, int score, int against) {
        V4PublicMatchTeamStatItemResp vo = new V4PublicMatchTeamStatItemResp();
        vo.setEventTeamId(eventTeamId);
        vo.setScore(score);
        vo.setPointsFor(score);
        vo.setPointsAgainst(against);
        return vo;
    }
}
