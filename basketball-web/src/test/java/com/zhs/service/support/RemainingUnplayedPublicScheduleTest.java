package com.zhs.service.support;

import com.zhs.model.v4.BasketMatchSeries;
import com.zhs.model.v4.BasketV4Match;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemainingUnplayedPublicScheduleTest {

    private final V4MatchSeriesDisplaySupport support = new V4MatchSeriesDisplaySupport();

    @Test
    void bo3SweepHidesGame3() {
        BasketMatchSeries series = completed(3, 2, 0);
        assertFalse(support.isRemainingUnplayedChild(official(1), series));
        assertFalse(support.isRemainingUnplayedChild(official(2), series));
        assertTrue(support.isRemainingUnplayedChild(scheduled(3), series));
    }

    @Test
    void bo3FullKeepsAllThreeGames() {
        BasketMatchSeries series = completed(3, 2, 1);
        assertFalse(support.isRemainingUnplayedChild(official(1), series));
        assertFalse(support.isRemainingUnplayedChild(official(2), series));
        assertFalse(support.isRemainingUnplayedChild(official(3), series));
    }

    @Test
    void bo5SweepHidesGame4AndGame5() {
        BasketMatchSeries series = completed(5, 3, 0);
        assertFalse(support.isRemainingUnplayedChild(official(1), series));
        assertFalse(support.isRemainingUnplayedChild(official(2), series));
        assertFalse(support.isRemainingUnplayedChild(official(3), series));
        assertTrue(support.isRemainingUnplayedChild(scheduled(4), series));
        assertTrue(support.isRemainingUnplayedChild(scheduled(5), series));
    }

    @Test
    void bo5FullKeepsAllFiveGames() {
        BasketMatchSeries series = completed(5, 3, 2);
        for (int gameNo = 1; gameNo <= 5; gameNo++) {
            assertFalse(support.isRemainingUnplayedChild(official(gameNo), series));
        }
    }

    @Test
    void openSeriesKeepsNextScheduledGame() {
        BasketMatchSeries series = new BasketMatchSeries();
        series.setSeriesStatus("IN_PROGRESS");
        series.setBestOf(5);
        series.setHomeWinCount(1);
        series.setAwayWinCount(1);
        assertFalse(support.isRemainingUnplayedChild(scheduled(3), series));
    }

    @Test
    void finishedHistoryStaysPublicAfterSeriesEnds() {
        assertFalse(support.isRemainingUnplayedChild(official(1), completed(7, 4, 2)));
    }

    private static BasketMatchSeries completed(int bestOf, int homeWins, int awayWins) {
        BasketMatchSeries series = new BasketMatchSeries();
        series.setSeriesStatus("COMPLETED");
        series.setBestOf(bestOf);
        series.setHomeWinCount(homeWins);
        series.setAwayWinCount(awayWins);
        series.setWinnerEventTeamId(17L);
        return series;
    }

    private static BasketV4Match official(int gameNo) {
        BasketV4Match match = new BasketV4Match();
        match.setSeriesId(1L);
        match.setSeriesGameNo(gameNo);
        match.setMatchStatus("FINISHED");
        return match;
    }

    private static BasketV4Match scheduled(int gameNo) {
        BasketV4Match match = new BasketV4Match();
        match.setSeriesId(1L);
        match.setSeriesGameNo(gameNo);
        match.setMatchStatus("SCHEDULED");
        return match;
    }
}