package com.zhs.service.support;

import com.zhs.response.publicapi.PublicHomeFocusTeamVo;
import com.zhs.response.publicapi.PublicHomePlayerLeaderVo;
import com.zhs.response.v4.publicapi.V4PublicMatchItemResp;
import com.zhs.response.v4.publicapi.V4PublicPlayerStatItemResp;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeRealtimeFocusSupportTest {

    private static final Date NOW = Date.from(
            ZonedDateTime.of(2026, 8, 26, 12, 0, 0, 0, HomeRealtimeFocusSupport.BUSINESS_ZONE).toInstant());
    private static final Date WEEK_START = HomeRealtimeFocusSupport.weekStartInclusive(NOW);
    private static final Date WEEK_END = HomeRealtimeFocusSupport.weekEndExclusive(NOW);

    @Test
    void blankSeriesCodeFallsBackToLeague() {
        assertEquals("LEAGUE", HomeRealtimeFocusSupport.normalizeSeriesCode(null));
        assertEquals("LEAGUE", HomeRealtimeFocusSupport.normalizeSeriesCode("  "));
        assertTrue(HomeRealtimeFocusSupport.isHomeSeriesCode("open"));
    }

    @Test
    void weekWindowIsMondayInclusiveNextMondayExclusive() {
        assertEquals(Date.from(ZonedDateTime.of(2026, 8, 24, 0, 0, 0, 0, HomeRealtimeFocusSupport.BUSINESS_ZONE).toInstant()),
                WEEK_START);
        assertEquals(Date.from(ZonedDateTime.of(2026, 8, 31, 0, 0, 0, 0, HomeRealtimeFocusSupport.BUSINESS_ZONE).toInstant()),
                WEEK_END);
        assertTrue(HomeRealtimeFocusSupport.inWeek(at(2026, 8, 24, 0), WEEK_START, WEEK_END));
        assertTrue(HomeRealtimeFocusSupport.inWeek(at(2026, 8, 30, 23), WEEK_START, WEEK_END));
        assertTrue(!HomeRealtimeFocusSupport.inWeek(at(2026, 8, 31, 0), WEEK_START, WEEK_END));
    }

    @Test
    void pointsLeaderUsesAvgPointsThenMatchCountThenEventPlayerId() {
        V4PublicPlayerStatItemResp low = player(3L, "8.0", 10, "5.0", "5.0");
        V4PublicPlayerStatItemResp tiedOlder = player(1L, "20.0", 4, "1.0", "1.0");
        V4PublicPlayerStatItemResp tiedNewer = player(2L, "20.0", 4, "9.0", "9.0");
        V4PublicPlayerStatItemResp moreGames = player(9L, "20.0", 6, "1.0", "1.0");
        PublicHomePlayerLeaderVo leader = HomeRealtimeFocusSupport.pickLeader(
                Arrays.asList(low, tiedNewer, tiedOlder, moreGames),
                V4PublicPlayerStatItemResp::getAvgPoints);
        assertEquals(9L, leader.getEventPlayerId());
        assertEquals(new BigDecimal("20.0"), leader.getValue());
    }

    @Test
    void assistsAndReboundsLeadersUseOwnAverages() {
        V4PublicPlayerStatItemResp scorer = player(1L, "30.0", 2, "1.0", "2.0");
        V4PublicPlayerStatItemResp passer = player(2L, "10.0", 2, "8.0", "1.0");
        V4PublicPlayerStatItemResp rebounder = player(3L, "10.0", 2, "2.0", "12.0");
        List<V4PublicPlayerStatItemResp> rows = Arrays.asList(scorer, passer, rebounder);
        assertEquals(1L, HomeRealtimeFocusSupport.pickLeader(rows, V4PublicPlayerStatItemResp::getAvgPoints).getEventPlayerId());
        assertEquals(2L, HomeRealtimeFocusSupport.pickLeader(rows, V4PublicPlayerStatItemResp::getAvgAssist).getEventPlayerId());
        assertEquals(3L, HomeRealtimeFocusSupport.pickLeader(rows, V4PublicPlayerStatItemResp::getAvgRebound).getEventPlayerId());
    }

    @Test
    void tiedAveragePrefersMoreGamesThenSmallerEventPlayerId() {
        V4PublicPlayerStatItemResp later = player(20L, "15.0", 3, "4.0", "4.0");
        V4PublicPlayerStatItemResp earlier = player(10L, "15.0", 3, "4.0", "4.0");
        PublicHomePlayerLeaderVo leader = HomeRealtimeFocusSupport.pickLeader(
                Arrays.asList(later, earlier), V4PublicPlayerStatItemResp::getAvgPoints);
        assertEquals(10L, leader.getEventPlayerId());
    }

    @Test
    void emptyPlayerStatReturnsNullLeader() {
        assertNull(HomeRealtimeFocusSupport.pickLeader(Collections.emptyList(), V4PublicPlayerStatItemResp::getAvgPoints));
    }

    @Test
    void moreWinsBeatsFewerWinsAtSameWinRate() {
        PublicHomeFocusTeamVo focus = HomeRealtimeFocusSupport.pickWeeklyFocusTeam(
                Arrays.asList(
                        finished(1L, 100L, 200L, 100L, 70, 60, at(2026, 8, 25, 12)),
                        finished(2L, 100L, 300L, 100L, 80, 70, at(2026, 8, 26, 12)),
                        finished(3L, 200L, 300L, 200L, 90, 80, at(2026, 8, 26, 18))),
                WEEK_START, WEEK_END, Collections.emptyMap());
        assertEquals(100L, focus.getEventTeamId());
        assertEquals(2, focus.getWeekWinCount());
        assertEquals(0, focus.getWeekLoseCount());
    }

    @Test
    void sameRecordPrefersLargerPointDiff() {
        PublicHomeFocusTeamVo focus = HomeRealtimeFocusSupport.pickWeeklyFocusTeam(
                Arrays.asList(
                        finished(1L, 100L, 200L, 100L, 70, 60, at(2026, 8, 25, 12)),
                        finished(2L, 300L, 400L, 300L, 90, 70, at(2026, 8, 26, 12))),
                WEEK_START, WEEK_END, Collections.emptyMap());
        assertEquals(300L, focus.getEventTeamId());
        assertEquals(new BigDecimal("20.0"), focus.getWeekAvgPointDiff());
    }

    @Test
    void noOfficialWeekMatchReturnsNullFocusTeam() {
        assertNull(HomeRealtimeFocusSupport.pickWeeklyFocusTeam(
                Arrays.asList(finished(1L, 100L, 200L, 100L, 70, 60, at(2026, 8, 20, 12))),
                WEEK_START, WEEK_END, Collections.emptyMap()));
    }

    @Test
    void forfeitedUsesWinnerEventTeamIdNotScore() {
        V4PublicMatchItemResp forfeited = match(1L, "FORFEITED", 100L, 200L, 100L, 0, 20, at(2026, 8, 25, 12));
        PublicHomeFocusTeamVo focus = HomeRealtimeFocusSupport.pickWeeklyFocusTeam(
                Collections.singletonList(forfeited), WEEK_START, WEEK_END, Collections.emptyMap());
        assertEquals(100L, focus.getEventTeamId());
        assertEquals(1, focus.getWeekWinCount());
        assertEquals(List.of("W"), focus.getRecentForm());
    }

    @Test
    void cancelledAndPlayingAreIgnored() {
        PublicHomeFocusTeamVo focus = HomeRealtimeFocusSupport.pickWeeklyFocusTeam(
                Arrays.asList(
                        match(1L, "CANCELLED", 100L, 200L, 100L, 80, 70, at(2026, 8, 25, 12)),
                        match(2L, "PLAYING", 100L, 200L, null, 10, 8, at(2026, 8, 26, 12)),
                        match(3L, "SCHEDULED", 100L, 200L, null, null, null, at(2026, 8, 27, 12))),
                WEEK_START, WEEK_END, Collections.emptyMap());
        assertNull(focus);
    }

    @Test
    void recentFormKeepsAtMostFiveOfficialResultsOldestFirst() {
        List<V4PublicMatchItemResp> matches = Arrays.asList(
                finished(1L, 100L, 200L, 100L, 70, 60, at(2026, 8, 10, 12)),
                finished(2L, 100L, 200L, 200L, 60, 70, at(2026, 8, 11, 12)),
                finished(3L, 100L, 200L, 100L, 75, 60, at(2026, 8, 12, 12)),
                finished(4L, 100L, 200L, 100L, 80, 70, at(2026, 8, 13, 12)),
                finished(5L, 100L, 200L, 200L, 50, 60, at(2026, 8, 14, 12)),
                finished(6L, 100L, 200L, 100L, 90, 80, at(2026, 8, 15, 12)));
        assertEquals(List.of("L", "W", "W", "L", "W"), HomeRealtimeFocusSupport.recentForm(matches, 100L));
    }

    @Test
    void otherDivisionMatchesDoNotAffectFocusTeam() {
        PublicHomeFocusTeamVo focus = HomeRealtimeFocusSupport.pickWeeklyFocusTeam(
                Collections.singletonList(finished(1L, 100L, 200L, 100L, 70, 60, at(2026, 8, 25, 12))),
                WEEK_START, WEEK_END, Map.of(900L, 1));
        assertEquals(100L, focus.getEventTeamId());
        assertEquals(1, focus.getWeekMatchCount());
    }

    private static V4PublicPlayerStatItemResp player(
            Long eventPlayerId, String avgPoints, int matchCount, String avgAssist, String avgRebound) {
        V4PublicPlayerStatItemResp row = new V4PublicPlayerStatItemResp();
        row.setEventPlayerId(eventPlayerId);
        row.setMatchCount(matchCount);
        row.setAvgPoints(new BigDecimal(avgPoints));
        row.setAvgAssist(new BigDecimal(avgAssist));
        row.setAvgRebound(new BigDecimal(avgRebound));
        return row;
    }

    private static V4PublicMatchItemResp finished(
            Long matchId, Long home, Long away, Long winner, Integer homeScore, Integer awayScore, Date matchTime) {
        return match(matchId, "FINISHED", home, away, winner, homeScore, awayScore, matchTime);
    }

    private static V4PublicMatchItemResp match(
            Long matchId, String status, Long home, Long away, Long winner,
            Integer homeScore, Integer awayScore, Date matchTime) {
        V4PublicMatchItemResp row = new V4PublicMatchItemResp();
        row.setMatchId(matchId);
        row.setMatchStatus(status);
        row.setHomeEventTeamId(home);
        row.setAwayEventTeamId(away);
        row.setWinnerEventTeamId(winner);
        row.setHomeScore(homeScore);
        row.setAwayScore(awayScore);
        row.setMatchTime(matchTime);
        row.setHomeTeamName("T" + home);
        row.setAwayTeamName("T" + away);
        return row;
    }

    private static Date at(int year, int month, int day, int hour) {
        return Date.from(ZonedDateTime.of(year, month, day, hour, 0, 0, 0, HomeRealtimeFocusSupport.BUSINESS_ZONE).toInstant());
    }
}
