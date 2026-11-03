package com.zhs.basket.support.v4.leaderboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 从单场正式记录汇总资格输入。弃赛、取消和轮空不产生技术局。
 * 同一个系列赛的多局只贡献一个 seriesId。
 */
public final class LeaderboardAggregator {

    private LeaderboardAggregator() {
    }

    public static List<LeaderboardSubject> aggregate(Collection<LeaderboardFacts.MatchFact> matches,
                                                     Collection<LeaderboardFacts.TeamBox> teamBoxes,
                                                     Collection<LeaderboardFacts.PlayerBox> playerBoxes,
                                                     Set<Long> stageIds,
                                                     Date now) {
        List<LeaderboardFacts.MatchFact> scoped = new ArrayList<>();
        for (LeaderboardFacts.MatchFact match : matches) {
            if (match == null || match.getId() == null || !"PUBLISHED".equals(match.getPublishStatus())) {
                continue;
            }
            if (stageIds != null && (match.getStageId() == null || !stageIds.contains(match.getStageId()))) {
                continue;
            }
            scoped.add(match);
        }
        Map<Long, LeaderboardFacts.MatchFact> byId = new LinkedHashMap<>();
        for (LeaderboardFacts.MatchFact match : scoped) {
            byId.putIfAbsent(match.getId(), match);
        }

        Set<Long> matchesWithBox = new HashSet<>();
        Map<Long, List<LeaderboardFacts.TeamBox>> teamByMatch = new HashMap<>();
        if (teamBoxes != null) {
            for (LeaderboardFacts.TeamBox box : teamBoxes) {
                if (box == null || box.getMatchId() == null || box.getEventTeamId() == null) {
                    continue;
                }
                if (!byId.containsKey(box.getMatchId())) {
                    continue;
                }
                matchesWithBox.add(box.getMatchId());
                teamByMatch.computeIfAbsent(box.getMatchId(), ignored -> new ArrayList<>()).add(box);
            }
        }
        Map<Long, List<LeaderboardFacts.PlayerBox>> playerByMatch = new HashMap<>();
        if (playerBoxes != null) {
            for (LeaderboardFacts.PlayerBox box : playerBoxes) {
                if (box == null || box.getMatchId() == null || box.getEventPlayerId() == null) {
                    continue;
                }
                if (!byId.containsKey(box.getMatchId())) {
                    continue;
                }
                playerByMatch.computeIfAbsent(box.getMatchId(), ignored -> new ArrayList<>()).add(box);
            }
        }

        Map<Long, MutableTeam> teams = new LinkedHashMap<>();
        Map<Long, MutablePlayer> players = new LinkedHashMap<>();
        Date clock = now == null ? new Date() : now;
        for (LeaderboardFacts.MatchFact match : byId.values()) {
            boolean technical = "FINISHED".equals(match.getMatchStatus()) && matchesWithBox.contains(match.getId());
            touchTeam(teams, match.getHomeEventTeamId(), match, technical, teamByMatch.get(match.getId()), clock);
            touchTeam(teams, match.getAwayEventTeamId(), match, technical, teamByMatch.get(match.getId()), clock);
            if (!technical) {
                continue;
            }
            List<LeaderboardFacts.PlayerBox> boxes = playerByMatch.get(match.getId());
            if (boxes == null) {
                continue;
            }
            for (LeaderboardFacts.PlayerBox box : boxes) {
                MutablePlayer player = players.computeIfAbsent(box.getEventPlayerId(), id -> new MutablePlayer(id, box.getEventTeamId()));
                if (player.matches.add(match.getId())) {
                    player.validGames++;
                    if (match.getRoundNo() != null) {
                        player.rounds.add(match.getRoundNo());
                    }
                    if (match.getSeriesId() != null) {
                        player.series.add(match.getSeriesId());
                    }
                    player.points += n(box.getPoints());
                    player.rebound += n(box.getRebound());
                    player.assist += n(box.getAssist());
                    player.steal += n(box.getSteal());
                    player.block += n(box.getBlock());
                    player.turnover += n(box.getTurnover());
                    player.fgMade += n(box.getFieldGoalMade());
                    player.fgAtt += n(box.getFieldGoalAttempt());
                    player.tpMade += n(box.getThreePointMade());
                    player.tpAtt += n(box.getThreePointAttempt());
                    player.ftMade += n(box.getFreeThrowMade());
                    player.ftAtt += n(box.getFreeThrowAttempt());
                }
            }
        }

        List<LeaderboardSubject> subjects = new ArrayList<>();
        for (MutableTeam team : teams.values()) {
            subjects.add(team.toSubject());
        }
        for (MutablePlayer player : players.values()) {
            MutableTeam team = teams.get(player.eventTeamId);
            LeaderboardSubject subject = player.toSubject();
            subject.setRequiredGames(team == null ? 0 : team.validGames);
            subjects.add(subject);
        }
        subjects.sort(Comparator.comparing(LeaderboardSubject::getSubjectType).thenComparing(LeaderboardSubject::getEventTeamId,
                Comparator.nullsLast(Long::compareTo)).thenComparing(LeaderboardSubject::playerKey));
        return subjects;
    }

    private static void touchTeam(Map<Long, MutableTeam> teams, Long teamId, LeaderboardFacts.MatchFact match,
                                  boolean technical, List<LeaderboardFacts.TeamBox> boxes, Date now) {
        if (teamId == null) {
            return;
        }
        MutableTeam team = teams.computeIfAbsent(teamId, MutableTeam::new);
        if ("CANCELLED".equals(match.getMatchStatus())) {
            return;
        }
        boolean future = match.getMatchTime() != null && match.getMatchTime().after(now)
                && !"FINISHED".equals(match.getMatchStatus())
                && !"FORFEITED".equals(match.getMatchStatus());
        if (!future) {
            team.due++;
            if ("FINISHED".equals(match.getMatchStatus()) || "FORFEITED".equals(match.getMatchStatus())) {
                team.fulfilled++;
            }
        }
        if (!technical || !team.matches.add(match.getId())) {
            return;
        }
        team.validGames++;
        if (match.getRoundNo() != null) {
            team.rounds.add(match.getRoundNo());
        }
        if (match.getSeriesId() != null) {
            team.series.add(match.getSeriesId());
        }
        if (boxes == null) {
            return;
        }
        boolean applied = false;
        for (LeaderboardFacts.TeamBox box : boxes) {
            if (!teamId.equals(box.getEventTeamId()) || applied) {
                continue;
            }
            applied = true;
            team.points += n(box.getPointsFor());
            team.rebound += n(box.getRebound());
            team.assist += n(box.getAssist());
            team.steal += n(box.getSteal());
            team.block += n(box.getBlock());
            team.turnover += n(box.getTurnover());
            team.fgMade += n(box.getFieldGoalMade());
            team.fgAtt += n(box.getFieldGoalAttempt());
            team.tpMade += n(box.getThreePointMade());
            team.tpAtt += n(box.getThreePointAttempt());
            team.ftMade += n(box.getFreeThrowMade());
            team.ftAtt += n(box.getFreeThrowAttempt());
            if (Integer.valueOf(1).equals(box.getIsWinner())) {
                team.wins++;
            } else {
                team.losses++;
            }
        }
    }

    private static int n(Integer value) {
        return value == null ? 0 : value;
    }

    private static final class MutableTeam {
        private final Long id;
        private final Set<Long> matches = new HashSet<>();
        private final Set<Integer> rounds = new HashSet<>();
        private final Set<Long> series = new HashSet<>();
        private int validGames;
        private int due;
        private int fulfilled;
        private int points;
        private int rebound;
        private int assist;
        private int steal;
        private int block;
        private int turnover;
        private int wins;
        private int losses;
        private int fgMade;
        private int fgAtt;
        private int tpMade;
        private int tpAtt;
        private int ftMade;
        private int ftAtt;

        private MutableTeam(Long id) {
            this.id = id;
        }

        private LeaderboardSubject toSubject() {
            LeaderboardSubject subject = new LeaderboardSubject();
            subject.setSubjectType(LeaderboardSubject.TEAM);
            subject.setEventTeamId(id);
            subject.setEventPlayerId(0L);
            subject.setValidGames(validGames);
            subject.setPlayedRounds(rounds.size());
            subject.setPlayedSeries(series.size());
            if (due > 0) {
                subject.setDueFixtureRate(BigDecimal.valueOf(fulfilled)
                        .divide(BigDecimal.valueOf(due), 4, RoundingMode.HALF_UP));
            }
            copy(subject);
            return subject;
        }

        private void copy(LeaderboardSubject subject) {
            subject.setPoints(points);
            subject.setRebound(rebound);
            subject.setAssist(assist);
            subject.setSteal(steal);
            subject.setBlock(block);
            subject.setTurnover(turnover);
            subject.setWins(wins);
            subject.setLosses(losses);
            subject.setFieldGoalMade(fgMade);
            subject.setFieldGoalAttempt(fgAtt);
            subject.setThreePointMade(tpMade);
            subject.setThreePointAttempt(tpAtt);
            subject.setFreeThrowMade(ftMade);
            subject.setFreeThrowAttempt(ftAtt);
        }
    }

    private static final class MutablePlayer {
        private final Long id;
        private final Long eventTeamId;
        private final Set<Long> matches = new HashSet<>();
        private final Set<Integer> rounds = new HashSet<>();
        private final Set<Long> series = new HashSet<>();
        private int validGames;
        private int points;
        private int rebound;
        private int assist;
        private int steal;
        private int block;
        private int turnover;
        private int fgMade;
        private int fgAtt;
        private int tpMade;
        private int tpAtt;
        private int ftMade;
        private int ftAtt;

        private MutablePlayer(Long id, Long eventTeamId) {
            this.id = id;
            this.eventTeamId = eventTeamId;
        }

        private LeaderboardSubject toSubject() {
            LeaderboardSubject subject = new LeaderboardSubject();
            subject.setSubjectType(LeaderboardSubject.PLAYER);
            subject.setEventTeamId(eventTeamId);
            subject.setEventPlayerId(id);
            subject.setValidGames(validGames);
            subject.setPlayedRounds(rounds.size());
            subject.setPlayedSeries(series.size());
            subject.setPoints(points);
            subject.setRebound(rebound);
            subject.setAssist(assist);
            subject.setSteal(steal);
            subject.setBlock(block);
            subject.setTurnover(turnover);
            subject.setFieldGoalMade(fgMade);
            subject.setFieldGoalAttempt(fgAtt);
            subject.setThreePointMade(tpMade);
            subject.setThreePointAttempt(tpAtt);
            subject.setFreeThrowMade(ftMade);
            subject.setFreeThrowAttempt(ftAtt);
            return subject;
        }
    }
}
