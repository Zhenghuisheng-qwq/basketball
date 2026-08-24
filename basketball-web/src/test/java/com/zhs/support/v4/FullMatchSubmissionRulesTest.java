package com.zhs.support.v4;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhs.exception.ServiceException;
import com.zhs.request.v4.V4MatchSubmissionPeriodScoreReq;
import com.zhs.request.v4.V4MatchSubmissionPlayerStatReq;
import com.zhs.response.v4.V4MatchSubmissionDetailResp;
import com.zhs.response.v4.V4SeriesSubmissionGameResp;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FullMatchSubmissionRulesTest {

    @Test
    void ptsFormula_fgm10_3pm4_ftm5_is29() {
        assertEquals(29, FullMatchSubmissionRules.expectedPoints(10, 4, 5));
    }

    @Test
    void ptsFormula_fgm7_3pm7_ftm0_is21() {
        assertEquals(21, FullMatchSubmissionRules.expectedPoints(7, 7, 0));
    }

    @Test
    void ptsFormula_fgm1_3pm0_is2() {
        assertEquals(2, FullMatchSubmissionRules.expectedPoints(1, 0, 0));
    }

    @Test
    void ptsFormula_fgm1_3pm1_is3() {
        assertEquals(3, FullMatchSubmissionRules.expectedPoints(1, 1, 0));
    }

    @Test
    void ptsFormula_fgm2_3pm1_ftm1_is6() {
        assertEquals(6, FullMatchSubmissionRules.expectedPoints(2, 1, 1));
    }

    @Test
    void ptsFormula_allZero() {
        assertEquals(0, FullMatchSubmissionRules.expectedPoints(0, 0, 0));
    }

    @Test
    void ptsFormula_oldSemanticsWouldBe5_newIs3() {
        assertEquals(3, FullMatchSubmissionRules.expectedPoints(1, 1, 0));
        org.junit.jupiter.api.Assertions.assertNotEquals(5, FullMatchSubmissionRules.expectedPoints(1, 1, 0));
    }

    @Test
    void shooting_allThreesOk_whenFgmIncludes3pm() {
        V4MatchSubmissionPlayerStatReq row = player(1L, 21, 7, 9, 7, 9, 0, 0);
        assertDoesNotThrow(() -> FullMatchSubmissionRules.validatePlayerRow("G1 · Home · Atlas", row, true));
    }

    @Test
    void shooting_ptsMismatchNewFormula() {
        V4MatchSubmissionPlayerStatReq row = player(1L, 18, 4, 6, 3, 4, 2, 2);
        assertThrows(ServiceException.class, () -> FullMatchSubmissionRules.validatePlayerRow("G1 · Home · Atlas", row, true));
    }

    @Test
    void shooting_mixedOk() {
        V4MatchSubmissionPlayerStatReq row = player(1L, 13, 4, 6, 3, 4, 2, 2);
        assertDoesNotThrow(() -> FullMatchSubmissionRules.validatePlayerRow("G1 · Home · Atlas", row, true));
    }

    @Test
    void shooting_threePointGreaterThanFgmFails() {
        V4MatchSubmissionPlayerStatReq row = player(1L, 12, 3, 5, 4, 4, 0, 0);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> FullMatchSubmissionRules.validatePlayerRow("G1 · Home · Atlas", row, true));
        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("3PM 4 大于 FGM 3"));
    }

    @Test
    void shooting_threePointAttemptGreaterThanFgaFails() {
        V4MatchSubmissionPlayerStatReq row = player(1L, 0, 0, 5, 0, 7, 0, 0);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> FullMatchSubmissionRules.validatePlayerRow("G1 · Home · Atlas", row, true));
        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("3PA 7 大于 FGA 5"));
    }

    @Test
    void shooting_fgm10_3pm4_ftm5_pts29_pass() {
        V4MatchSubmissionPlayerStatReq row = player(1L, 29, 10, 12, 4, 6, 5, 5);
        assertDoesNotThrow(() -> FullMatchSubmissionRules.validatePlayerRow("G1 · Home · Atlas", row, true));
    }

    @Test
    void shooting_fgm1_3pm1_pts3_pass() {
        V4MatchSubmissionPlayerStatReq row = player(1L, 3, 1, 1, 1, 1, 0, 0);
        assertDoesNotThrow(() -> FullMatchSubmissionRules.validatePlayerRow("G1 · Home · Atlas", row, true));
    }

    @Test
    void shooting_fgm1_3pm1_pts5_failOldFormula() {
        V4MatchSubmissionPlayerStatReq row = player(1L, 5, 1, 1, 1, 1, 0, 0);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> FullMatchSubmissionRules.validatePlayerRow("G1 · Home · Atlas", row, true));
        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("(FGM-3PM)×2+3PM×3+FTM=3"));
    }

    @Test
    void period_skippedFor3v3() {
        assertDoesNotThrow(() ->
                FullMatchSubmissionRules.validatePeriodScores("G1", List.of(), 21, 15, true, false));
    }

    @Test
    void period_requiredFor5v5() {
        assertThrows(ServiceException.class, () ->
                FullMatchSubmissionRules.validatePeriodScores("G1", List.of(), 21, 15, true, true));
    }

    @Test
    void requiresPeriodScores_byGameMode() {
        org.junit.jupiter.api.Assertions.assertFalse(FullMatchSubmissionRules.requiresPeriodScores("THREE_V_THREE"));
        org.junit.jupiter.api.Assertions.assertFalse(FullMatchSubmissionRules.requiresPeriodScores("3V3"));
        org.junit.jupiter.api.Assertions.assertTrue(FullMatchSubmissionRules.requiresPeriodScores("FIVE_V_FIVE"));
        org.junit.jupiter.api.Assertions.assertTrue(FullMatchSubmissionRules.requiresPeriodScores("5V5"));
        org.junit.jupiter.api.Assertions.assertTrue(FullMatchSubmissionRules.requiresPeriodScores(null));
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
    void placeholderPairing_cannotSubmit() {
        org.junit.jupiter.api.Assertions.assertFalse(FullMatchSubmissionRules.isPairingResolved(null, 8L));
        ServiceException ex = assertThrows(ServiceException.class,
                () -> FullMatchSubmissionRules.assertPairingResolved(null, null));
        assertEquals(FullMatchSubmissionRules.MSG_PAIRING_UNRESOLVED, ex.getMessage());
        assertDoesNotThrow(() -> FullMatchSubmissionRules.assertPairingResolved(1L, 8L));
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

    @Test
    void zeroScore_legalWhenFormulaHolds_0_to_21() {
        V4MatchSubmissionPlayerStatReq zero = player(1L, 0, 0, 5, 0, 5, 0, 0);
        V4MatchSubmissionPlayerStatReq twentyOne = player(2L, 21, 7, 9, 7, 9, 0, 0);
        assertDoesNotThrow(() -> FullMatchSubmissionRules.validatePlayerRow("G1 · A · Atlas", zero, true));
        assertDoesNotThrow(() -> FullMatchSubmissionRules.validatePlayerRow("G1 · B · Atlas", twentyOne, true));
        assertDoesNotThrow(() -> FullMatchSubmissionRules.validateScoreCross("G1", 0, 21, 0, 21));
        assertDoesNotThrow(() -> FullMatchSubmissionRules.validateScoreCross("G1", 21, 0, 21, 0));
    }

    @Test
    void zeroPts_withTwoPointMade_failsFormula() {
        V4MatchSubmissionPlayerStatReq row = player(1L, 0, 1, 1, 0, 0, 0, 0);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> FullMatchSubmissionRules.validatePlayerRow("G1 · A · Atlas", row, true));
        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("(FGM-3PM)×2+3PM×3+FTM=2"));
    }

    @Test
    void zeroPts_withThreePointMade_failsFormula() {
        V4MatchSubmissionPlayerStatReq row = player(1L, 0, 1, 1, 1, 1, 0, 0);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> FullMatchSubmissionRules.validatePlayerRow("G1 · A · Atlas", row, true));
        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("(FGM-3PM)×2+3PM×3+FTM=3"));
    }

    @Test
    void zeroPts_withFreeThrowMade_failsFormula() {
        V4MatchSubmissionPlayerStatReq row = player(1L, 0, 0, 0, 0, 0, 1, 1);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> FullMatchSubmissionRules.validatePlayerRow("G1 · A · Atlas", row, true));
        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("(FGM-3PM)×2+3PM×3+FTM=1"));
    }

    @Test
    void fiveV5_zeroHomeAllQuarters_21Away_pass() {
        List<V4MatchSubmissionPeriodScoreReq> periods = Arrays.asList(
                q(1, 0, 5), q(2, 0, 5), q(3, 0, 5), q(4, 0, 6));
        assertDoesNotThrow(() -> FullMatchSubmissionRules.validatePeriodScores("G1", periods, 0, 21, true, true));
        assertDoesNotThrow(() -> FullMatchSubmissionRules.validateScoreCross("G1", 0, 21, 0, 21));
    }

    @Test
    void threeV3_zeroToTwentyOne_skipsQuarters() {
        assertDoesNotThrow(() ->
                FullMatchSubmissionRules.validatePeriodScores("G1", List.of(), 0, 21, true, false));
        assertDoesNotThrow(() -> FullMatchSubmissionRules.validateScoreCross("G1", 0, 21, 0, 21));
    }

    @Test
    void zeroToZero_stillRejectedAsDraw() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> FullMatchSubmissionRules.validateScoreCross("G1", 0, 0, 0, 0));
        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("不允许平局"));
    }

    @Test
    void materialFileIds_serializeAsStrings() throws Exception {
        long snowflakeId = 199999999999999999L;
        V4MatchSubmissionDetailResp resp = new V4MatchSubmissionDetailResp();
        resp.setMaterialFileIds(List.of(snowflakeId));
        String json = new ObjectMapper().writeValueAsString(resp);
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"" + snowflakeId + "\""));
    }

    @Test
    void seriesMaterialFileIds_serializeAsStrings() throws Exception {
        long snowflakeId = 188888888888888888L;
        V4SeriesSubmissionGameResp resp = new V4SeriesSubmissionGameResp();
        resp.setMaterialFileIds(List.of(snowflakeId));
        String json = new ObjectMapper().writeValueAsString(resp);
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"" + snowflakeId + "\""));
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
