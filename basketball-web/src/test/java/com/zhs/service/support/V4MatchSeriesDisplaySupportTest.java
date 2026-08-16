package com.zhs.service.support;

import com.zhs.model.v4.BasketMatchSeries;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class V4MatchSeriesDisplaySupportTest {

    private final V4MatchSeriesDisplaySupport support = new V4MatchSeriesDisplaySupport();

    @Test
    void forfeitSweep_bo1() {
        assertArrayEquals(new int[]{1, 0}, resolve(1, 10L));
        assertArrayEquals(new int[]{0, 1}, resolve(1, 20L));
    }

    @Test
    void forfeitSweep_bo3() {
        assertArrayEquals(new int[]{2, 0}, resolve(3, 10L));
        assertArrayEquals(new int[]{0, 2}, resolve(3, 20L));
    }

    @Test
    void forfeitSweep_bo5() {
        assertArrayEquals(new int[]{3, 0}, resolve(5, 10L));
        assertArrayEquals(new int[]{0, 3}, resolve(5, 20L));
    }

    @Test
    void forfeitSweep_bo7() {
        assertArrayEquals(new int[]{4, 0}, resolve(7, 10L));
        assertArrayEquals(new int[]{0, 4}, resolve(7, 20L));
    }

    @Test
    void normalSeriesKeepsPlayedScore() {
        BasketMatchSeries series = series(3, 10L);
        series.setHomeWinCount(2);
        series.setAwayWinCount(1);
        series.setResolutionType(null);
        series.setSeriesStatus("COMPLETED");
        assertArrayEquals(new int[]{2, 1}, support.resolveDisplayWinCounts(series));
        assertEquals("2:1", support.formatSeriesScore(series, 2, 1));
    }

    @Test
    void unfinishedSeriesWithoutWinsHidesScore() {
        BasketMatchSeries series = series(5, null);
        series.setHomeWinCount(0);
        series.setAwayWinCount(0);
        series.setResolutionType(null);
        series.setSeriesStatus("READY");
        assertArrayEquals(new int[]{0, 0}, support.resolveDisplayWinCounts(series));
        assertNull(support.formatSeriesScore(series, 0, 0));
    }

    private int[] resolve(int bestOf, Long winner) {
        BasketMatchSeries series = series(bestOf, winner);
        series.setHomeWinCount(0);
        series.setAwayWinCount(0);
        series.setResolutionType("FORFEIT");
        series.setSeriesStatus("COMPLETED");
        return support.resolveDisplayWinCounts(series);
    }

    private BasketMatchSeries series(int bestOf, Long winner) {
        BasketMatchSeries series = new BasketMatchSeries();
        series.setBestOf(bestOf);
        series.setHomeEventTeamId(10L);
        series.setAwayEventTeamId(20L);
        series.setWinnerEventTeamId(winner);
        return series;
    }
}