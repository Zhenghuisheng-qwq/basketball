package com.zhs.basket.support.v4.leaderboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LeaderboardEligibilityEngine {

    public static final String TEAM_GAMES_TOO_FEW = "TEAM_GAMES_TOO_FEW";
    public static final String TEAM_ROUNDS_TOO_FEW = "TEAM_ROUNDS_TOO_FEW";
    public static final String TEAM_SERIES_TOO_FEW = "TEAM_SERIES_TOO_FEW";
    public static final String TEAM_DUE_FIXTURE_RATE_LOW = "TEAM_DUE_FIXTURE_RATE_LOW";
    public static final String TEAM_NOT_ELIGIBLE = "TEAM_NOT_ELIGIBLE";
    public static final String PLAYER_GAMES_TOO_FEW = "PLAYER_GAMES_TOO_FEW";
    public static final String PLAYER_ROUNDS_TOO_FEW = "PLAYER_ROUNDS_TOO_FEW";
    public static final String PLAYER_SERIES_TOO_FEW = "PLAYER_SERIES_TOO_FEW";
    public static final String NO_VALID_TEAM_GAMES = "NO_VALID_TEAM_GAMES";

    private LeaderboardEligibilityEngine() {
    }

    public static void apply(List<LeaderboardSubject> subjects, LeaderboardConfig config) {
        Map<Long, LeaderboardSubject> teams = new HashMap<>();
        for (LeaderboardSubject subject : subjects) {
            if (LeaderboardSubject.TEAM.equals(subject.getSubjectType())) {
                evaluateTeam(subject, config);
                teams.put(subject.getEventTeamId(), subject);
            }
        }
        for (LeaderboardSubject subject : subjects) {
            if (LeaderboardSubject.PLAYER.equals(subject.getSubjectType())) {
                evaluatePlayer(subject, teams.get(subject.getEventTeamId()), config);
            }
        }
    }

    public static void evaluateTeam(LeaderboardSubject team, LeaderboardConfig config) {
        List<String> failed = new ArrayList<>();
        LeaderboardConfig.TeamRule rule = config.getTeam();
        if (team.getValidGames() < rule.getMinimumTeamGames()) {
            failed.add(TEAM_GAMES_TOO_FEW);
        }
        if (rule.getMinimumTeamPlayedRounds() > 0 && team.getPlayedRounds() < rule.getMinimumTeamPlayedRounds()) {
            failed.add(TEAM_ROUNDS_TOO_FEW);
        }
        if (rule.getMinimumTeamPlayedSeries() > 0 && team.getPlayedSeries() < rule.getMinimumTeamPlayedSeries()) {
            failed.add(TEAM_SERIES_TOO_FEW);
        }
        if (rule.getMinimumDueFixtureRate() != null) {
            BigDecimal actual = team.getDueFixtureRate() == null ? BigDecimal.ZERO : team.getDueFixtureRate();
            if (team.getDueFixtureRate() != null && actual.compareTo(rule.getMinimumDueFixtureRate()) < 0) {
                failed.add(TEAM_DUE_FIXTURE_RATE_LOW);
            }
        }
        finish(team, failed, config);
    }

    public static void evaluatePlayer(LeaderboardSubject player, LeaderboardSubject team, LeaderboardConfig config) {
        List<String> failed = new ArrayList<>();
        LeaderboardConfig.PlayerRule rule = config.getPlayer();
        int denominator = player.getRequiredGames();
        if (team != null) {
            denominator = team.getValidGames();
        }
        player.setRequiredGames(requiredGames(rule, denominator));
        if (rule.isRequireEligibleTeam() && (team == null || !team.isEligible())) {
            failed.add(TEAM_NOT_ELIGIBLE);
        }
        if (denominator <= 0) {
            failed.add(NO_VALID_TEAM_GAMES);
        }
        if (player.getValidGames() < player.getRequiredGames()) {
            failed.add(PLAYER_GAMES_TOO_FEW);
        }
        if (rule.getMinimumPlayerPlayedRounds() > 0 && player.getPlayedRounds() < rule.getMinimumPlayerPlayedRounds()) {
            failed.add(PLAYER_ROUNDS_TOO_FEW);
        }
        if (rule.getMinimumPlayerPlayedSeries() > 0 && player.getPlayedSeries() < rule.getMinimumPlayerPlayedSeries()) {
            failed.add(PLAYER_SERIES_TOO_FEW);
        }
        finish(player, failed, config);
    }

    public static int requiredGames(LeaderboardConfig.PlayerRule rule, int teamGames) {
        BigDecimal rate = rule.getMinimumPlayerTeamRate() == null ? BigDecimal.ZERO : rule.getMinimumPlayerTeamRate();
        int fromRate = BigDecimal.valueOf(Math.max(teamGames, 0)).multiply(rate)
                .setScale(0, RoundingMode.CEILING).intValue();
        return Math.max(rule.getMinimumPlayerGames(), fromRate);
    }

    private static void finish(LeaderboardSubject subject, List<String> failed, LeaderboardConfig config) {
        subject.setFailedConditions(failed);
        boolean openBoard = LeaderboardScopes.ALL_PARTICIPANTS.equals(config.getRankingAudience());
        subject.setEligible(openBoard || failed.isEmpty());
    }
}
