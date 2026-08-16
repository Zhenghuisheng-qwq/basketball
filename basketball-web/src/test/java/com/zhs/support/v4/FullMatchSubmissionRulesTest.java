package com.zhs.support.v4;

import com.zhs.exception.ServiceException;
import com.zhs.request.v4.V4MatchSubmissionPeriodScoreReq;
import com.zhs.request.v4.V4MatchSubmissionPlayerStatReq;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FullMatchSubmissionRulesTest {

    @Test
    void ptsFormula_21() {
        assertEquals(21, FullMatchSubmissionRules.expectedPoints(8, 3, 2));
    }

    @Test
    void seriesScore_bo5_3_1() {
        assertDoesNotThrow(() -> FullMatchSubmissionRules.assertValidSeriesScore(5, 3, 1));
        assertEquals(4, FullMatchSubmissionRules.actualGameCount(5, 3, 1));
    }

    @Test
    void seriesScore_invalid() {
        assertThrows(ServiceException.class, () -> FullMatchSubmissionRules.assertValidSeriesScore(5, 2, 1));
    }

    @Test
    void shooting_fgmGreaterThanFga() {
        V4MatchSubmissionPlayerStatReq row = player(1L, 20, 9, 8, 3, 3, 2, 2);
        assertThrows(ServiceException.class, () -> FullMatchSubmissionRules.validatePlayerRow("G3 · Team A · Atlas", row, true));
    }

    @Test
    void shooting_ptsMismatch() {
        V4MatchSubmissionPlayerStatReq row = player(1L, 20, 8, 8, 3, 3, 2, 2);
        assertThrows(ServiceException.class, () -> FullMatchSubmissionRules.validatePlayerRow("G3 · Team A · Atlas", row, true));
    }

    @Test
    void shooting_ok() {
        V4MatchSubmissionPlayerStatReq row = player(1L, 21, 8, 10, 3, 4, 2, 2);
        assertDoesNotThrow(() -> FullMatchSubmissionRules.validatePlayerRow("G1 · Home · Atlas", row, true));
    }

    @Test
    void duplicatePlayer() {
        V4MatchSubmissionPlayerStatReq a = player(11L, 21, 8, 10, 3, 4, 2, 2);
        V4MatchSubmissionPlayerStatReq b = player(11L, 10, 4, 6, 1, 2, 1, 1);
        assertThrows(ServiceException.class,
                () -> FullMatchSubmissionRules.validateTeamPlayers("G1", "Home", 100L, Arrays.asList(a, b), true));
    }

    @Test
    void periodScore_mismatch() {
        List<V4MatchSubmissionPeriodScoreReq> periods = Arrays.asList(q(1, 10, 8), q(2, 10, 8), q(3, 10, 8), q(4, 10, 8));
        assertThrows(ServiceException.class,
                () -> FullMatchSubmissionRules.validatePeriodScores("G1", periods, 50, 32, true));
    }

    @Test
    void periodScore_ok() {
        List<V4MatchSubmissionPeriodScoreReq> periods = Arrays.asList(q(1, 10, 8), q(2, 10, 8), q(3, 10, 8), q(4, 10, 8));
        assertDoesNotThrow(() -> FullMatchSubmissionRules.validatePeriodScores("G1", periods, 40, 32, true));
    }

    @Test
    void winnerFullMatchAlias() {
        org.junit.jupiter.api.Assertions.assertTrue(FullMatchSubmissionRules.isWinnerFullMatch("WINNER_FULL_MATCH"));
        org.junit.jupiter.api.Assertions.assertTrue(FullMatchSubmissionRules.isWinnerFullMatch("FULL_MATCH"));
        org.junit.jupiter.api.Assertions.assertFalse(FullMatchSubmissionRules.isWinnerFullMatch("LEGACY_TEAM_SIDE"));
    }

    @Test
    void seriesScore_bo3_2_0() {
        assertDoesNotThrow(() -> FullMatchSubmissionRules.assertValidSeriesScore(3, 2, 0));
        assertEquals(2, FullMatchSubmissionRules.actualGameCount(3, 2, 0));
    }

    @Test
    void emptyRowIgnored() {
        V4MatchSubmissionPlayerStatReq blank = new V4MatchSubmissionPlayerStatReq();
        assertEquals(0, FullMatchSubmissionRules.usedRows(List.of(blank)).size());
    }

    private V4MatchSubmissionPlayerStatReq player(Long id, int pts, int fgm, int fga, int tpm, int tpa, int ftm, int fta) {
        V4MatchSubmissionPlayerStatReq row = new V4MatchSubmissionPlayerStatReq();
        row.setEventPlayerId(id);
        row.setEventTeamId(100L);
        row.setPlayerName("Atlas");
        row.setPoints(pts);
        row.setFieldGoalMade(fgm);
        row.setFieldGoalAttempt(fga);
        row.setThreePointMade(tpm);
        row.setThreePointAttempt(tpa);
        row.setFreeThrowMade(ftm);
        row.setFreeThrowAttempt(fta);
        return row;
    }

    private V4MatchSubmissionPeriodScoreReq q(int no, int home, int away) {
        V4MatchSubmissionPeriodScoreReq req = new V4MatchSubmissionPeriodScoreReq();
        req.setPeriodNo(no);
        req.setHomeScore(home);
        req.setAwayScore(away);
        return req;
    }
}
