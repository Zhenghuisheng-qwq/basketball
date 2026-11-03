package com.zhs.basket.support.v4.leaderboard;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderboardEligibilityEngineTest {

    @Test
    void twoGamesInOneRoundStayBelowTwoRoundMinimum() {
        List<LeaderboardFacts.MatchFact> matches = List.of(
                match(1L, 10L, 1, null, "FINISHED"),
                match(2L, 10L, 1, null, "FINISHED"));
        List<LeaderboardFacts.TeamBox> boxes = List.of(box(1L, 100L, 20), box(2L, 100L, 18));
        List<LeaderboardSubject> subjects = LeaderboardAggregator.aggregate(matches, boxes, List.of(), Set.of(10L), new Date());
        LeaderboardConfig config = config(0, 2, null);
        LeaderboardEligibilityEngine.apply(subjects, config);
        LeaderboardSubject team = team(subjects, 100L);
        assertEquals(2, team.getValidGames());
        assertEquals(1, team.getPlayedRounds());
        assertFalse(team.isEligible());
        assertTrue(team.getFailedConditions().contains(LeaderboardEligibilityEngine.TEAM_ROUNDS_TOO_FEW));
    }

    @Test
    void duplicateOfficialBoxCountsAsOneGame() {
        LeaderboardFacts.MatchFact match = match(7L, 10L, 1, 70L, "FINISHED");
        List<LeaderboardFacts.TeamBox> boxes = List.of(box(7L, 100L, 21), box(7L, 100L, 99));
        List<LeaderboardSubject> subjects = LeaderboardAggregator.aggregate(
                List.of(match, match), boxes, List.of(), Set.of(10L), new Date());
        LeaderboardSubject team = team(subjects, 100L);
        assertEquals(1, team.getValidGames());
        assertEquals(1, team.getPlayedSeries());
        assertEquals(21, team.getPoints());
    }

    @Test
    void forfeitAndMissingByeDoNotCreateTechnicalGames() {
        LeaderboardFacts.MatchFact forfeited = match(3L, 10L, 1, null, "FORFEITED");
        List<LeaderboardFacts.TeamBox> boxes = List.of(box(3L, 100L, 0));
        List<LeaderboardSubject> subjects = LeaderboardAggregator.aggregate(
                List.of(forfeited), boxes, List.of(), Set.of(10L), new Date());
        LeaderboardSubject team = team(subjects, 100L);
        assertEquals(0, team.getValidGames());
        assertEquals(0, team.getPoints());
        assertEquals(new BigDecimal("1.0000"), team.getDueFixtureRate());
    }

    @Test
    void overallKeepsTeamThatOnlyPlayedTheGroupStage() {
        List<LeaderboardFacts.MatchFact> matches = List.of(match(4L, 11L, 1, null, "FINISHED"));
        List<LeaderboardSubject> subjects = LeaderboardAggregator.aggregate(
                matches, List.of(box(4L, 100L, 30)), List.of(), Set.of(11L, 12L), new Date());
        LeaderboardConfig config = config(1, 0, null);
        LeaderboardEligibilityEngine.apply(subjects, config);
        assertTrue(team(subjects, 100L).isEligible());
    }

    @Test
    void shootingGateDoesNotRemovePlayerFromPointsBoard() {
        LeaderboardFacts.MatchFact match = match(5L, 10L, 1, null, "FINISHED");
        LeaderboardFacts.PlayerBox player = new LeaderboardFacts.PlayerBox();
        player.setMatchId(5L);
        player.setEventTeamId(100L);
        player.setEventPlayerId(501L);
        player.setPoints(8);
        player.setThreePointMade(1);
        player.setThreePointAttempt(1);
        List<LeaderboardSubject> subjects = LeaderboardAggregator.aggregate(
                List.of(match), List.of(box(5L, 100L, 20)), List.of(player), Set.of(10L), new Date());
        LeaderboardConfig config = config(1, 0, null);
        LeaderboardConfig.MetricGate three = new LeaderboardConfig.MetricGate();
        three.setEnabled(true);
        three.setMinimumAttemptsPerAppearance(2);
        config.getMetricGates().put("THREE_PCT", three);
        LeaderboardEligibilityEngine.apply(subjects, config);
        List<LeaderboardSubject> points = LeaderboardRanker.rank(copy(subjects), "PTS", "total", config);
        List<LeaderboardSubject> threes = LeaderboardRanker.rank(copy(subjects), "THREE_PCT", "total", config);
        assertEquals(1, points.stream().filter(row -> Long.valueOf(501L).equals(row.getEventPlayerId())).count());
        assertEquals(0, threes.stream().filter(row -> Long.valueOf(501L).equals(row.getEventPlayerId())).count());
    }

    @Test
    void averageAndTotalKeepTheSameEligiblePool() {
        List<LeaderboardFacts.MatchFact> matches = new ArrayList<>();
        List<LeaderboardFacts.TeamBox> boxes = new ArrayList<>();
        matches.add(match(11L, 10L, 1, null, "FINISHED", 100L, 900L));
        matches.add(match(12L, 10L, 1, null, "FINISHED", 100L, 901L));
        boxes.add(box(11L, 100L, 10));
        boxes.add(box(12L, 100L, 10));
        long opponent = 910L;
        for (long id = 21L; id <= 24L; id++) {
            matches.add(match(id, 10L, 1, null, "FINISHED", 200L, opponent++));
            boxes.add(box(id, 200L, 8));
        }
        List<LeaderboardSubject> subjects = LeaderboardAggregator.aggregate(matches, boxes, List.of(), Set.of(10L), new Date());
        LeaderboardConfig config = config(2, 0, null);
        LeaderboardEligibilityEngine.apply(subjects, config);
        List<LeaderboardSubject> average = LeaderboardRanker.rank(teamsOnly(copy(subjects)), "PTS", "average", config);
        List<LeaderboardSubject> total = LeaderboardRanker.rank(teamsOnly(copy(subjects)), "PTS", "total", config);
        assertEquals(2, average.size());
        assertEquals(2, total.size());
        assertEquals(100L, average.get(0).getEventTeamId());
        assertEquals(200L, total.get(0).getEventTeamId());
        assertNull(playerRank(subjects));
    }

    @Test
    void lowDueFixtureRateFailsAndFutureGameIsNotDue() {
        Date now = new Date(1_000_000L);
        LeaderboardFacts.MatchFact played = match(31L, 10L, 1, null, "FINISHED");
        LeaderboardFacts.MatchFact missed = match(32L, 10L, 1, null, "SCHEDULED");
        LeaderboardFacts.MatchFact future = match(33L, 10L, 1, null, "SCHEDULED");
        future.setMatchTime(new Date(now.getTime() + 86_400_000L));
        List<LeaderboardSubject> subjects = LeaderboardAggregator.aggregate(
                List.of(played, missed, future), List.of(box(31L, 100L, 10)), List.of(), Set.of(10L), now);
        LeaderboardSubject team = team(subjects, 100L);
        assertEquals(new BigDecimal("0.5000"), team.getDueFixtureRate());
        LeaderboardConfig config = config(1, 0, new BigDecimal("0.70"));
        LeaderboardEligibilityEngine.apply(subjects, config);
        assertFalse(team.isEligible());
        assertTrue(team.getFailedConditions().contains(LeaderboardEligibilityEngine.TEAM_DUE_FIXTURE_RATE_LOW));
    }

    private static LeaderboardFacts.MatchFact match(Long id, Long stageId, Integer round, Long seriesId, String status) {
        return match(id, stageId, round, seriesId, status, 100L, 200L);
    }

    private static LeaderboardFacts.MatchFact match(Long id, Long stageId, Integer round, Long seriesId, String status,
                                                    Long home, Long away) {
        LeaderboardFacts.MatchFact match = new LeaderboardFacts.MatchFact();
        match.setId(id);
        match.setStageId(stageId);
        match.setRoundNo(round);
        match.setSeriesId(seriesId);
        match.setHomeEventTeamId(home);
        match.setAwayEventTeamId(away);
        match.setMatchStatus(status);
        match.setPublishStatus("PUBLISHED");
        return match;
    }

    private static LeaderboardFacts.TeamBox box(Long matchId, Long teamId, int points) {
        LeaderboardFacts.TeamBox box = new LeaderboardFacts.TeamBox();
        box.setMatchId(matchId);
        box.setEventTeamId(teamId);
        box.setPointsFor(points);
        box.setIsWinner(points >= 20 ? 1 : 0);
        return box;
    }

    private static LeaderboardConfig config(int games, int rounds, BigDecimal dueRate) {
        LeaderboardConfig config = new LeaderboardConfig();
        config.getTeam().setMinimumTeamGames(games);
        config.getTeam().setMinimumTeamPlayedRounds(rounds);
        config.getTeam().setMinimumDueFixtureRate(dueRate);
        config.getPlayer().setMinimumPlayerGames(1);
        config.getPlayer().setMinimumPlayerTeamRate(BigDecimal.ZERO);
        return config;
    }

    private static LeaderboardSubject team(List<LeaderboardSubject> subjects, Long teamId) {
        return subjects.stream()
                .filter(row -> LeaderboardSubject.TEAM.equals(row.getSubjectType()) && teamId.equals(row.getEventTeamId()))
                .findFirst()
                .orElseThrow();
    }

    private static Integer playerRank(List<LeaderboardSubject> subjects) {
        return subjects.stream()
                .filter(row -> LeaderboardSubject.PLAYER.equals(row.getSubjectType()))
                .map(LeaderboardSubject::getRankNo)
                .findFirst()
                .orElse(null);
    }

    private static List<LeaderboardSubject> teamsOnly(List<LeaderboardSubject> subjects) {
        List<LeaderboardSubject> teams = new ArrayList<>();
        for (LeaderboardSubject subject : subjects) {
            if (LeaderboardSubject.TEAM.equals(subject.getSubjectType())
                    && (Long.valueOf(100L).equals(subject.getEventTeamId()) || Long.valueOf(200L).equals(subject.getEventTeamId()))) {
                teams.add(subject);
            }
        }
        return teams;
    }

    private static List<LeaderboardSubject> copy(List<LeaderboardSubject> subjects) {
        List<LeaderboardSubject> copy = new ArrayList<>();
        for (LeaderboardSubject subject : subjects) {
            LeaderboardSubject next = new LeaderboardSubject();
            next.setSubjectType(subject.getSubjectType());
            next.setEventTeamId(subject.getEventTeamId());
            next.setEventPlayerId(subject.getEventPlayerId());
            next.setValidGames(subject.getValidGames());
            next.setPlayedRounds(subject.getPlayedRounds());
            next.setPlayedSeries(subject.getPlayedSeries());
            next.setDueFixtureRate(subject.getDueFixtureRate());
            next.setRequiredGames(subject.getRequiredGames());
            next.setEligible(subject.isEligible());
            next.setFailedConditions(new ArrayList<>(subject.getFailedConditions()));
            next.setPoints(subject.getPoints());
            next.setThreePointMade(subject.getThreePointMade());
            next.setThreePointAttempt(subject.getThreePointAttempt());
            copy.add(next);
        }
        return copy;
    }
}
