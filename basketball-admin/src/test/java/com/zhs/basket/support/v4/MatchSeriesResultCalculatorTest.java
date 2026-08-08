package com.zhs.basket.support.v4;

import com.zhs.basket.enums.v4.MatchStatusEnum;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchSeriesResultCalculatorTest {

    private static final long HOME = 101L;
    private static final long AWAY = 202L;

    @Test
    void caseA_bo3_homeHome_2_0_completed() {
        MatchSeriesResultSnapshot result = MatchSeriesResultCalculator.calculate(3, HOME, AWAY, List.of(
                game(1, HOME), game(2, HOME)));
        assertEquals(2, result.getRequiredWins());
        assertEquals(2, result.getHomeWinCount());
        assertEquals(0, result.getAwayWinCount());
        assertEquals(HOME, result.getWinnerEventTeamId());
        assertTrue(result.isCompleted());
    }

    @Test
    void caseA_bo3_extraGame_ignoredAfterCompletion() {
        MatchSeriesResultSnapshot result = MatchSeriesResultCalculator.calculate(3, HOME, AWAY, List.of(
                game(1, HOME), game(2, HOME), game(3, AWAY)));
        assertEquals(2, result.getHomeWinCount());
        assertEquals(0, result.getAwayWinCount());
        assertEquals(HOME, result.getWinnerEventTeamId());
        assertTrue(result.isCompleted());
    }

    @Test
    void caseB_bo3_homeAwayHome_2_1() {
        MatchSeriesResultSnapshot result = MatchSeriesResultCalculator.calculate(3, HOME, AWAY, List.of(
                game(1, HOME), game(2, AWAY), game(3, HOME)));
        assertEquals(2, result.getHomeWinCount());
        assertEquals(1, result.getAwayWinCount());
        assertEquals(HOME, result.getWinnerEventTeamId());
        assertTrue(result.isCompleted());
    }

    @Test
    void caseC_bo5_3_2() {
        MatchSeriesResultSnapshot result = MatchSeriesResultCalculator.calculate(5, HOME, AWAY, List.of(
                game(1, HOME), game(2, AWAY), game(3, HOME), game(4, AWAY), game(5, HOME)));
        assertEquals(3, result.getRequiredWins());
        assertEquals(3, result.getHomeWinCount());
        assertEquals(2, result.getAwayWinCount());
        assertEquals(HOME, result.getWinnerEventTeamId());
        assertTrue(result.isCompleted());
    }

    @Test
    void caseD_bo5_0_3_awayWinner() {
        MatchSeriesResultSnapshot result = MatchSeriesResultCalculator.calculate(5, HOME, AWAY, List.of(
                game(1, AWAY), game(2, AWAY), game(3, AWAY)));
        assertEquals(0, result.getHomeWinCount());
        assertEquals(3, result.getAwayWinCount());
        assertEquals(AWAY, result.getWinnerEventTeamId());
        assertTrue(result.isCompleted());
    }

    @Test
    void caseE_bo5_incomplete_1_1() {
        MatchSeriesResultSnapshot result = MatchSeriesResultCalculator.calculate(5, HOME, AWAY, List.of(
                game(1, HOME), game(2, AWAY)));
        assertEquals(1, result.getHomeWinCount());
        assertEquals(1, result.getAwayWinCount());
        assertNull(result.getWinnerEventTeamId());
        assertFalse(result.isCompleted());
    }

    @Test
    void caseF_invalidBestOf2() {
        assertThrows(ServiceException.class, () -> MatchSeriesResultCalculator.requiredWins(2));
        assertThrows(ServiceException.class, () -> MatchSeriesResultCalculator.calculate(2, HOME, AWAY, List.of()));
    }

    @Test
    void caseG_homeEqualsAway() {
        assertThrows(ServiceException.class, () -> MatchSeriesResultCalculator.calculate(3, HOME, HOME, List.of()));
    }

    @Test
    void caseH_duplicateGameNo() {
        assertThrows(ServiceException.class, () -> MatchSeriesResultCalculator.calculate(3, HOME, AWAY, List.of(
                game(1, HOME), game(1, AWAY))));
    }

    @Test
    void requiredWins_formula() {
        assertEquals(2, MatchSeriesResultCalculator.requiredWins(3));
        assertEquals(3, MatchSeriesResultCalculator.requiredWins(5));
    }

    @Test
    void ignoresNonEffectiveMatchStatus() {
        MatchSeriesGameInput draft = game(1, HOME);
        draft.setMatchStatus(MatchStatusEnum.DRAFT.getCode());
        MatchSeriesResultSnapshot result = MatchSeriesResultCalculator.calculate(3, HOME, AWAY, List.of(draft));
        assertEquals(0, result.getHomeWinCount());
        assertFalse(result.isCompleted());
    }

    private MatchSeriesGameInput game(int gameNo, long winner) {
        MatchSeriesGameInput input = new MatchSeriesGameInput();
        input.setSeriesGameNo(gameNo);
        input.setHomeEventTeamId(HOME);
        input.setAwayEventTeamId(AWAY);
        input.setWinnerEventTeamId(winner);
        input.setMatchStatus(MatchStatusEnum.FINISHED.getCode());
        return input;
    }
}
