package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.*;
import com.zhs.basket.enums.v4.MatchStatusEnum;
import com.zhs.basket.enums.v4.RankTypeEnum;
import com.zhs.basket.mapper.v4.*;
import com.zhs.basket.service.v4.IBasketV4StatsSettlementService;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BasketV4StatsSettlementServiceImpl implements IBasketV4StatsSettlementService {

    private static final Integer NOT_DELETED = 0;
    private static final int WIN_POINTS = 2;
    private static final int LOSE_POINTS = 1;

    @Autowired
    private BasketMatchSubmissionMapper submissionMapper;
    @Autowired
    private BasketMatchSubmissionTeamStatMapper submissionTeamStatMapper;
    @Autowired
    private BasketMatchSubmissionPlayerStatMapper submissionPlayerStatMapper;
    @Autowired
    private BasketV4MatchMapper matchMapper;
    @Autowired
    private BasketV4MatchTeamStatMapper matchTeamStatMapper;
    @Autowired
    private BasketV4MatchPlayerStatMapper matchPlayerStatMapper;
    @Autowired
    private BasketEventTeamMapper eventTeamMapper;
    @Autowired
    private BasketStandingMapper standingMapper;
    @Autowired
    private BasketTeamStatMapper teamStatMapper;
    @Autowired
    private BasketPlayerStatMapper playerStatMapper;
    @Autowired
    private BasketRankMapper rankMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settleBySubmission(Long submissionId) {
        BasketMatchSubmission submission = requireSubmission(submissionId);
        applyOfficialMatchResult(submission);
        rebuildDivisionStats(submission.getDivisionId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rebuildDivisionStats(Long divisionId) {
        if (divisionId == null) {
            throw new ServiceException("divisionId 不能为空");
        }
        BasketV4Match sample = matchMapper.selectFinishedPublishedByDivisionId(divisionId).stream().findFirst().orElse(null);
        Long seasonId = sample != null ? sample.getSeasonId() : null;
        if (seasonId == null) {
            BasketEventTeam team = eventTeamMapper.selectActiveByDivisionId(divisionId).stream().findFirst().orElse(null);
            seasonId = team != null ? team.getSeasonId() : null;
        }
        rebuildStanding(divisionId, seasonId);
        rebuildTeamStat(divisionId, seasonId);
        rebuildPlayerStat(divisionId, seasonId);
        rebuildRank(divisionId, seasonId);
    }

    private void applyOfficialMatchResult(BasketMatchSubmission submission) {
        BasketV4Match match = requireMatch(submission.getMatchId());
        if (submission.getHomeScore() == null || submission.getAwayScore() == null) {
            throw new ServiceException("提交缺少总比分");
        }
        Date now = new Date();
        match.setHomeScore(submission.getHomeScore());
        match.setAwayScore(submission.getAwayScore());
        match.setWinnerEventTeamId(resolveWinner(match.getHomeEventTeamId(), match.getAwayEventTeamId(),
                submission.getHomeScore(), submission.getAwayScore()));
        match.setMatchStatus(MatchStatusEnum.FINISHED.getCode());
        match.setUpdateTime(now);
        if (matchMapper.updateById(match) != 1) {
            throw new ServiceException("更新官方比赛结果失败");
        }

        BasketMatchSubmissionTeamStat subTeamStat = submissionTeamStatMapper.selectBySubmissionId(submission.getId());
        upsertSubmitTeamMatchStat(submission, match, subTeamStat, now);
        upsertOpponentMinimalTeamStat(submission, match, now);

        List<BasketMatchSubmissionPlayerStat> subPlayerStats = submissionPlayerStatMapper.selectBySubmissionId(submission.getId());
        for (BasketMatchSubmissionPlayerStat ps : subPlayerStats) {
            upsertMatchPlayerStat(submission, match, ps, now);
        }
    }

    private void upsertSubmitTeamMatchStat(BasketMatchSubmission submission, BasketV4Match match,
                                           BasketMatchSubmissionTeamStat subTeamStat, Date now) {
        boolean isHome = submission.getSubmitEventTeamId().equals(match.getHomeEventTeamId());
        int teamScore = isHome ? submission.getHomeScore() : submission.getAwayScore();
        int oppScore = isHome ? submission.getAwayScore() : submission.getHomeScore();
        BasketV4MatchTeamStat existing = matchTeamStatMapper.selectByMatchAndEventTeam(match.getId(), submission.getSubmitEventTeamId());
        BasketV4MatchTeamStat row = existing != null ? existing : new BasketV4MatchTeamStat();
        row.setMatchId(match.getId());
        row.setSeasonId(match.getSeasonId());
        row.setDivisionId(match.getDivisionId());
        row.setEventTeamId(submission.getSubmitEventTeamId());
        row.setOpponentEventTeamId(submission.getOpponentEventTeamId());
        row.setIsWinner(teamScore > oppScore ? 1 : 0);
        row.setScore(teamScore);
        row.setPointsFor(teamScore);
        row.setPointsAgainst(oppScore);
        if (subTeamStat != null) {
            row.setFieldGoalMade(nvl(subTeamStat.getFieldGoalMade()));
            row.setFieldGoalAttempt(nvl(subTeamStat.getFieldGoalAttempt()));
            row.setThreePointMade(nvl(subTeamStat.getThreePointMade()));
            row.setThreePointAttempt(nvl(subTeamStat.getThreePointAttempt()));
            row.setFreeThrowMade(nvl(subTeamStat.getFreeThrowMade()));
            row.setFreeThrowAttempt(nvl(subTeamStat.getFreeThrowAttempt()));
            row.setRebound(nvl(subTeamStat.getRebound()));
            row.setAssist(nvl(subTeamStat.getAssist()));
            row.setSteal(nvl(subTeamStat.getSteal()));
            row.setBlock(nvl(subTeamStat.getBlock()));
            row.setTurnover(nvl(subTeamStat.getTurnover()));
            row.setFoul(nvl(subTeamStat.getFoul()));
        }
        applyTeamStatDefaults(row);
        row.setSourceSubmissionId(submission.getId());
        row.setUpdateTime(now);
        if (existing == null) {
            row.setCreateTime(now);
            row.setIsDelete(NOT_DELETED);
            matchTeamStatMapper.insert(row);
        } else {
            matchTeamStatMapper.updateById(row);
        }
    }

    private void upsertOpponentMinimalTeamStat(BasketMatchSubmission submission, BasketV4Match match, Date now) {
        Long oppTeamId = submission.getOpponentEventTeamId();
        if (oppTeamId == null) {
            return;
        }
        boolean submitIsHome = submission.getSubmitEventTeamId().equals(match.getHomeEventTeamId());
        int oppScore = submitIsHome ? submission.getAwayScore() : submission.getHomeScore();
        int teamScore = submitIsHome ? submission.getHomeScore() : submission.getAwayScore();
        BasketV4MatchTeamStat existing = matchTeamStatMapper.selectByMatchAndEventTeam(match.getId(), oppTeamId);
        if (existing != null && existing.getSourceSubmissionId() != null
                && !existing.getSourceSubmissionId().equals(submission.getId())) {
            return;
        }
        BasketV4MatchTeamStat row = existing != null ? existing : new BasketV4MatchTeamStat();
        row.setMatchId(match.getId());
        row.setSeasonId(match.getSeasonId());
        row.setDivisionId(match.getDivisionId());
        row.setEventTeamId(oppTeamId);
        row.setOpponentEventTeamId(submission.getSubmitEventTeamId());
        row.setIsWinner(oppScore > teamScore ? 1 : 0);
        row.setScore(oppScore);
        row.setPointsFor(oppScore);
        row.setPointsAgainst(teamScore);
        row.setSourceSubmissionId(submission.getId());
        applyTeamStatDefaults(row);
        row.setUpdateTime(now);
        if (existing == null) {
            row.setCreateTime(now);
            row.setIsDelete(NOT_DELETED);
            matchTeamStatMapper.insert(row);
        } else {
            matchTeamStatMapper.updateById(row);
        }
    }

    private void upsertMatchPlayerStat(BasketMatchSubmission submission, BasketV4Match match,
                                       BasketMatchSubmissionPlayerStat ps, Date now) {
        BasketV4MatchPlayerStat existing = matchPlayerStatMapper.selectByMatchAndEventPlayer(match.getId(), ps.getEventPlayerId());
        BasketV4MatchPlayerStat row = existing != null ? existing : new BasketV4MatchPlayerStat();
        row.setMatchId(match.getId());
        row.setSeasonId(match.getSeasonId());
        row.setDivisionId(match.getDivisionId());
        row.setEventTeamId(ps.getEventTeamId());
        row.setEventPlayerId(ps.getEventPlayerId());
        row.setPoints(nvl(ps.getPoints()));
        row.setScore(nvl(ps.getPoints()));
        row.setRebound(nvl(ps.getRebound()));
        row.setAssist(nvl(ps.getAssist()));
        row.setSteal(nvl(ps.getSteal()));
        row.setBlock(nvl(ps.getBlock()));
        row.setTurnover(nvl(ps.getTurnover()));
        row.setFoul(nvl(ps.getFoul()));
        row.setMinutes(ps.getMinutes());
        row.setSourceSubmissionId(submission.getId());
        row.setUpdateTime(now);
        if (existing == null) {
            row.setCreateTime(now);
            row.setIsDelete(NOT_DELETED);
            matchPlayerStatMapper.insert(row);
        } else {
            matchPlayerStatMapper.updateById(row);
        }
    }

    private void rebuildStanding(Long divisionId, Long seasonId) {
        List<BasketV4Match> matches = matchMapper.selectFinishedPublishedByDivisionId(divisionId);
        List<BasketEventTeam> teams = eventTeamMapper.selectActiveByDivisionId(divisionId);
        Map<Long, StandingAgg> aggMap = new LinkedHashMap<>();
        for (BasketEventTeam team : teams) {
            aggMap.put(team.getId(), new StandingAgg(team.getId(), seasonId, divisionId));
        }
        for (BasketV4Match match : matches) {
            if (match.getHomeScore() == null || match.getAwayScore() == null) {
                continue;
            }
            applyMatchToStanding(aggMap, match.getHomeEventTeamId(), match.getAwayEventTeamId(),
                    match.getHomeScore(), match.getAwayScore());
        }
        List<StandingAgg> sorted = aggMap.values().stream()
                .sorted(Comparator.comparingInt(StandingAgg::getWinCount).reversed()
                        .thenComparing(Comparator.comparingInt(StandingAgg::getPointDiff).reversed())
                        .thenComparing(Comparator.comparingInt(StandingAgg::getPointFor).reversed())
                        .thenComparing(StandingAgg::getEventTeamId))
                .collect(Collectors.toList());
        standingMapper.deleteByDivisionId(divisionId);
        Date now = new Date();
        int rank = 1;
        for (StandingAgg agg : sorted) {
            BasketStanding row = new BasketStanding();
            row.setSeasonId(seasonId);
            row.setDivisionId(divisionId);
            row.setEventTeamId(agg.eventTeamId);
            row.setRankNo(rank++);
            row.setWinCount(agg.winCount);
            row.setLoseCount(agg.loseCount);
            row.setDrawCount(0);
            row.setPointFor(agg.pointFor);
            row.setPointAgainst(agg.pointAgainst);
            row.setPointDiff(agg.pointFor - agg.pointAgainst);
            row.setStandingPoints(agg.standingPoints);
            row.setCreateTime(now);
            row.setUpdateTime(now);
            row.setIsDelete(NOT_DELETED);
            standingMapper.insert(row);
        }
    }

    private void applyMatchToStanding(Map<Long, StandingAgg> aggMap, Long homeId, Long awayId, int homeScore, int awayScore) {
        StandingAgg home = aggMap.computeIfAbsent(homeId, id -> new StandingAgg(id, null, null));
        StandingAgg away = aggMap.computeIfAbsent(awayId, id -> new StandingAgg(id, null, null));
        home.pointFor += homeScore;
        home.pointAgainst += awayScore;
        away.pointFor += awayScore;
        away.pointAgainst += homeScore;
        if (homeScore > awayScore) {
            home.winCount++;
            home.standingPoints += WIN_POINTS;
            away.loseCount++;
            away.standingPoints += LOSE_POINTS;
        } else if (awayScore > homeScore) {
            away.winCount++;
            away.standingPoints += WIN_POINTS;
            home.loseCount++;
            home.standingPoints += LOSE_POINTS;
        }
    }

    private void rebuildTeamStat(Long divisionId, Long seasonId) {
        List<BasketV4MatchTeamStat> rows = matchTeamStatMapper.selectByDivisionId(divisionId);
        Map<Long, TeamStatAgg> aggMap = new HashMap<>();
        for (BasketV4MatchTeamStat row : rows) {
            TeamStatAgg agg = aggMap.computeIfAbsent(row.getEventTeamId(), TeamStatAgg::new);
            agg.matchCount++;
            if (Integer.valueOf(1).equals(row.getIsWinner())) {
                agg.winCount++;
            } else {
                agg.loseCount++;
            }
            agg.pointsFor += nvl(row.getPointsFor());
            agg.pointsAgainst += nvl(row.getPointsAgainst());
            agg.rebound += nvl(row.getRebound());
            agg.assist += nvl(row.getAssist());
            agg.steal += nvl(row.getSteal());
            agg.block += nvl(row.getBlock());
            agg.turnover += nvl(row.getTurnover());
        }
        teamStatMapper.deleteByDivisionId(divisionId);
        Date now = new Date();
        for (TeamStatAgg agg : aggMap.values()) {
            BasketTeamStat row = new BasketTeamStat();
            row.setSeasonId(seasonId);
            row.setDivisionId(divisionId);
            row.setEventTeamId(agg.eventTeamId);
            row.setMatchCount(agg.matchCount);
            row.setWinCount(agg.winCount);
            row.setLoseCount(agg.loseCount);
            row.setPointsFor(agg.pointsFor);
            row.setPointsAgainst(agg.pointsAgainst);
            row.setAvgPointsFor(avg(agg.pointsFor, agg.matchCount));
            row.setAvgPointsAgainst(avg(agg.pointsAgainst, agg.matchCount));
            row.setRebound(agg.rebound);
            row.setAssist(agg.assist);
            row.setSteal(agg.steal);
            row.setBlock(agg.block);
            row.setTurnover(agg.turnover);
            row.setCreateTime(now);
            row.setUpdateTime(now);
            row.setIsDelete(NOT_DELETED);
            teamStatMapper.insert(row);
        }
    }

    private void rebuildPlayerStat(Long divisionId, Long seasonId) {
        List<BasketV4MatchPlayerStat> rows = matchPlayerStatMapper.selectByDivisionId(divisionId);
        Map<Long, PlayerStatAgg> aggMap = new HashMap<>();
        for (BasketV4MatchPlayerStat row : rows) {
            PlayerStatAgg agg = aggMap.computeIfAbsent(row.getEventPlayerId(), id -> new PlayerStatAgg(row.getEventTeamId()));
            agg.matchCount++;
            agg.points += nvl(row.getPoints());
            agg.rebound += nvl(row.getRebound());
            agg.assist += nvl(row.getAssist());
            agg.steal += nvl(row.getSteal());
            agg.block += nvl(row.getBlock());
            agg.turnover += nvl(row.getTurnover());
        }
        playerStatMapper.deleteByDivisionId(divisionId);
        Date now = new Date();
        for (Map.Entry<Long, PlayerStatAgg> entry : aggMap.entrySet()) {
            PlayerStatAgg agg = entry.getValue();
            BasketPlayerStat row = new BasketPlayerStat();
            row.setSeasonId(seasonId);
            row.setDivisionId(divisionId);
            row.setEventTeamId(agg.eventTeamId);
            row.setEventPlayerId(entry.getKey());
            row.setMatchCount(agg.matchCount);
            row.setPoints(agg.points);
            row.setAvgPoints(avg(agg.points, agg.matchCount));
            row.setRebound(agg.rebound);
            row.setAvgRebound(avg(agg.rebound, agg.matchCount));
            row.setAssist(agg.assist);
            row.setAvgAssist(avg(agg.assist, agg.matchCount));
            row.setSteal(agg.steal);
            row.setBlock(agg.block);
            row.setTurnover(agg.turnover);
            row.setCreateTime(now);
            row.setUpdateTime(now);
            row.setIsDelete(NOT_DELETED);
            playerStatMapper.insert(row);
        }
    }

    private void rebuildRank(Long divisionId, Long seasonId) {
        rankMapper.deleteByDivisionId(divisionId);
        Date now = new Date();
        List<BasketStanding> standings = standingMapper.selectByDivisionId(divisionId);
        insertTeamStandingRanks(divisionId, seasonId, standings, now);
        List<BasketPlayerStat> playerStats = playerStatMapper.selectByDivisionId(divisionId);
        insertPlayerRanks(divisionId, seasonId, playerStats, RankTypeEnum.PLAYER_SCORE, ps -> BigDecimal.valueOf(nvl(ps.getPoints())), now);
        insertPlayerRanks(divisionId, seasonId, playerStats, RankTypeEnum.PLAYER_ASSIST, ps -> BigDecimal.valueOf(nvl(ps.getAssist())), now);
        insertPlayerRanks(divisionId, seasonId, playerStats, RankTypeEnum.PLAYER_REBOUND, ps -> BigDecimal.valueOf(nvl(ps.getRebound())), now);
    }

    private void insertTeamStandingRanks(Long divisionId, Long seasonId, List<BasketStanding> standings, Date now) {
        int rankNo = 1;
        for (BasketStanding s : standings) {
            BasketRank row = new BasketRank();
            row.setSeasonId(seasonId);
            row.setDivisionId(divisionId);
            row.setRankType(RankTypeEnum.TEAM_STANDING.getCode());
            row.setRankNo(rankNo++);
            row.setTargetType("TEAM");
            row.setEventTeamId(s.getEventTeamId());
            row.setRankValue(BigDecimal.valueOf(nvl(s.getStandingPoints())));
            row.setCreateTime(now);
            row.setUpdateTime(now);
            row.setIsDelete(NOT_DELETED);
            rankMapper.insert(row);
        }
    }

    private void insertPlayerRanks(Long divisionId, Long seasonId, List<BasketPlayerStat> playerStats,
                                   RankTypeEnum rankType, java.util.function.Function<BasketPlayerStat, BigDecimal> valueFn, Date now) {
        List<BasketPlayerStat> sorted = playerStats.stream()
                .sorted(Comparator.comparing((BasketPlayerStat ps) -> valueFn.apply(ps)).reversed()
                        .thenComparing(BasketPlayerStat::getEventPlayerId))
                .collect(Collectors.toList());
        int rankNo = 1;
        for (BasketPlayerStat ps : sorted) {
            BigDecimal val = valueFn.apply(ps);
            if (val.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BasketRank row = new BasketRank();
            row.setSeasonId(seasonId);
            row.setDivisionId(divisionId);
            row.setRankType(rankType.getCode());
            row.setRankNo(rankNo++);
            row.setTargetType("PLAYER");
            row.setEventTeamId(ps.getEventTeamId());
            row.setEventPlayerId(ps.getEventPlayerId());
            row.setRankValue(val);
            row.setCreateTime(now);
            row.setUpdateTime(now);
            row.setIsDelete(NOT_DELETED);
            rankMapper.insert(row);
        }
    }

    private Long resolveWinner(Long homeTeamId, Long awayTeamId, int homeScore, int awayScore) {
        if (homeScore > awayScore) {
            return homeTeamId;
        }
        if (awayScore > homeScore) {
            return awayTeamId;
        }
        return null;
    }

    private BasketMatchSubmission requireSubmission(Long id) {
        BasketMatchSubmission submission = submissionMapper.selectById(id);
        if (submission == null) {
            throw new ServiceException("提交不存在");
        }
        return submission;
    }

    private BasketV4Match requireMatch(Long id) {
        BasketV4Match match = matchMapper.selectById(id);
        if (match == null) {
            throw new ServiceException("比赛不存在");
        }
        return match;
    }

    private int nvl(Integer v) {
        return v == null ? 0 : v;
    }

    private void applyTeamStatDefaults(BasketV4MatchTeamStat row) {
        if (row.getFieldGoalMade() == null) {
            row.setFieldGoalMade(0);
        }
        if (row.getFieldGoalAttempt() == null) {
            row.setFieldGoalAttempt(0);
        }
        if (row.getThreePointMade() == null) {
            row.setThreePointMade(0);
        }
        if (row.getThreePointAttempt() == null) {
            row.setThreePointAttempt(0);
        }
        if (row.getFreeThrowMade() == null) {
            row.setFreeThrowMade(0);
        }
        if (row.getFreeThrowAttempt() == null) {
            row.setFreeThrowAttempt(0);
        }
        if (row.getRebound() == null) {
            row.setRebound(0);
        }
        if (row.getAssist() == null) {
            row.setAssist(0);
        }
        if (row.getSteal() == null) {
            row.setSteal(0);
        }
        if (row.getBlock() == null) {
            row.setBlock(0);
        }
        if (row.getTurnover() == null) {
            row.setTurnover(0);
        }
        if (row.getFoul() == null) {
            row.setFoul(0);
        }
    }

    private BigDecimal avg(int total, int count) {
        if (count <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(total).divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private static class StandingAgg {
        private final Long eventTeamId;
        private int winCount;
        private int loseCount;
        private int pointFor;
        private int pointAgainst;
        private int standingPoints;

        StandingAgg(Long eventTeamId, Long seasonId, Long divisionId) {
            this.eventTeamId = eventTeamId;
        }

        int getWinCount() { return winCount; }
        int getPointDiff() { return pointFor - pointAgainst; }
        int getPointFor() { return pointFor; }
        int getStandingPoints() { return standingPoints; }
        Long getEventTeamId() { return eventTeamId; }
    }

    private static class TeamStatAgg {
        private final Long eventTeamId;
        private int matchCount;
        private int winCount;
        private int loseCount;
        private int pointsFor;
        private int pointsAgainst;
        private int rebound;
        private int assist;
        private int steal;
        private int block;
        private int turnover;

        TeamStatAgg(Long eventTeamId) {
            this.eventTeamId = eventTeamId;
        }
    }

    private static class PlayerStatAgg {
        private final Long eventTeamId;
        private int matchCount;
        private int points;
        private int rebound;
        private int assist;
        private int steal;
        private int block;
        private int turnover;

        PlayerStatAgg(Long eventTeamId) {
            this.eventTeamId = eventTeamId;
        }
    }
}
