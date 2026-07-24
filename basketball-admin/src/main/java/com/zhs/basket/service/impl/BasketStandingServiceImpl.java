package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketStageStanding;
import com.zhs.basket.domain.BasketTournamentGroup;
import com.zhs.basket.domain.BasketTournamentSeason;
import com.zhs.basket.domain.BasketTournamentStage;
import com.zhs.basket.enums.MatchResultTypeEnum;
import com.zhs.basket.enums.StandingTieBreakEnum;
import com.zhs.basket.mapper.BasketConfirmedMatchAdminMapper;
import com.zhs.basket.mapper.BasketStageStandingAdminMapper;
import com.zhs.basket.mapper.BasketTournamentGroupMapper;
import com.zhs.basket.mapper.BasketTournamentSeasonMapper;
import com.zhs.basket.mapper.BasketTournamentStageMapper;
import com.zhs.basket.query.StandingQuery;
import com.zhs.basket.service.IBasketStandingService;
import com.zhs.basket.support.StageStandingSupport;
import com.zhs.basket.support.StandingPointsRule;
import com.zhs.basket.support.StatMathUtils;
import com.zhs.basket.vo.ConfirmedMatchBriefVo;
import com.zhs.basket.vo.StandingListVo;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class BasketStandingServiceImpl implements IBasketStandingService {

    private static final long STAGE_LEVEL_GROUP_ID = 0L;

    @Autowired
    private BasketStageStandingAdminMapper standingAdminMapper;
    @Autowired
    private BasketConfirmedMatchAdminMapper confirmedMatchAdminMapper;
    @Autowired
    private BasketTournamentSeasonMapper seasonMapper;
    @Autowired
    private BasketTournamentStageMapper stageMapper;
    @Autowired
    private BasketTournamentGroupMapper groupMapper;

    @Override
    public List<StandingListVo> selectList(StandingQuery query) {
        return standingAdminMapper.selectList(query);
    }

    @Override
    public int rebuildSeasonStandings(Long seasonId, StandingPointsRule rule, Date snapshotTime) {
        requireSeason(seasonId);
        requireRule(rule);
        Date snap = snapshotTime == null ? new Date() : snapshotTime;
        standingAdminMapper.softDeleteByScope(seasonId, null, null);
        List<Long> stageIds = standingAdminMapper.selectStageIdsBySeason(seasonId);
        int total = 0;
        if (CollectionUtils.isEmpty(stageIds)) {
            return 0;
        }
        for (Long stageId : stageIds) {
            total += rebuildStageInternal(stageId, rule, snap, false);
        }
        return total;
    }

    @Override
    public int rebuildStageStandings(Long stageId, StandingPointsRule rule, Date snapshotTime) {
        requireRule(rule);
        Date snap = snapshotTime == null ? new Date() : snapshotTime;
        return rebuildStageInternal(stageId, rule, snap, true);
    }

    @Override
    public int rebuildGroupStandings(Long groupId, StandingPointsRule rule, Date snapshotTime) {
        requireRule(rule);
        if (groupId == null) {
            throw new ServiceException("小组ID不能为空");
        }
        BasketTournamentGroup group = groupMapper.selectById(groupId);
        if (group == null || (group.getIsDelete() != null && group.getIsDelete() != 0)) {
            throw new ServiceException("小组不存在");
        }
        BasketTournamentStage stage = requireStage(group.getStageId());
        if (StageStandingSupport.isKnockoutStage(stage.getStageType(), stage.getFormatType())) {
            standingAdminMapper.softDeleteByScope(stage.getSeasonId(), stage.getId(), groupId);
            return 0;
        }
        Date snap = snapshotTime == null ? new Date() : snapshotTime;
        List<ConfirmedMatchBriefVo> stageMatches =
                confirmedMatchAdminMapper.selectConfirmedMatches(stage.getSeasonId(), stage.getId(), null);
        Set<Long> teamIds = new HashSet<>(standingAdminMapper.selectTeamIdsByGroup(groupId));
        List<ConfirmedMatchBriefVo> matches = filterGroupMatches(stageMatches, teamIds);
        standingAdminMapper.softDeleteByScope(stage.getSeasonId(), stage.getId(), groupId);
        List<BasketStageStanding> rows = buildStandings(
                stage.getSeasonId(), stage.getId(), groupId, matches, teamIds, rule, snap);
        if (rows.isEmpty()) {
            return 0;
        }
        standingAdminMapper.batchInsert(rows);
        return rows.size();
    }

    private int rebuildStageInternal(Long stageId, StandingPointsRule rule, Date snapshotTime, boolean softDelete) {
        BasketTournamentStage stage = requireStage(stageId);
        Long seasonId = stage.getSeasonId();
        if (StageStandingSupport.isKnockoutStage(stage.getStageType(), stage.getFormatType())) {
            if (softDelete) {
                standingAdminMapper.softDeleteByScope(seasonId, stageId, null);
            }
            return 0;
        }
        if (softDelete) {
            standingAdminMapper.softDeleteByScope(seasonId, stageId, null);
        }
        List<ConfirmedMatchBriefVo> stageMatches =
                confirmedMatchAdminMapper.selectConfirmedMatches(seasonId, stageId, null);
        List<Long> groupIds = standingAdminMapper.selectGroupIdsByStage(stageId);
        List<BasketStageStanding> all = new ArrayList<>();
        if (!CollectionUtils.isEmpty(groupIds)) {
            for (Long groupId : groupIds) {
                Set<Long> teamIds = new HashSet<>(standingAdminMapper.selectTeamIdsByGroup(groupId));
                List<ConfirmedMatchBriefVo> groupMatches = filterGroupMatches(stageMatches, teamIds);
                all.addAll(buildStandings(seasonId, stageId, groupId, groupMatches, teamIds, rule, snapshotTime));
            }
        }
        // group_id=0：无小组或阶段全量积分榜
        Set<Long> stageTeamIds = collectTeamIds(stageMatches);
        all.addAll(buildStandings(seasonId, stageId, STAGE_LEVEL_GROUP_ID, stageMatches, stageTeamIds, rule, snapshotTime));
        if (all.isEmpty()) {
            return 0;
        }
        standingAdminMapper.batchInsert(all);
        return all.size();
    }

    private List<ConfirmedMatchBriefVo> filterGroupMatches(List<ConfirmedMatchBriefVo> matches, Set<Long> teamIds) {
        List<ConfirmedMatchBriefVo> result = new ArrayList<>();
        if (CollectionUtils.isEmpty(matches) || CollectionUtils.isEmpty(teamIds)) {
            return result;
        }
        for (ConfirmedMatchBriefVo match : matches) {
            if (teamIds.contains(match.getHomeTeamId()) && teamIds.contains(match.getAwayTeamId())) {
                result.add(match);
            }
        }
        return result;
    }

    private Set<Long> collectTeamIds(List<ConfirmedMatchBriefVo> matches) {
        Set<Long> ids = new HashSet<>();
        if (CollectionUtils.isEmpty(matches)) {
            return ids;
        }
        for (ConfirmedMatchBriefVo match : matches) {
            if (match.getHomeTeamId() != null) {
                ids.add(match.getHomeTeamId());
            }
            if (match.getAwayTeamId() != null) {
                ids.add(match.getAwayTeamId());
            }
        }
        return ids;
    }

    private List<BasketStageStanding> buildStandings(Long seasonId, Long stageId, Long groupId,
                                                     List<ConfirmedMatchBriefVo> matches,
                                                     Set<Long> seedTeamIds,
                                                     StandingPointsRule rule,
                                                     Date snapshotTime) {
        Map<Long, TeamAgg> aggMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(seedTeamIds)) {
            for (Long teamId : seedTeamIds) {
                aggMap.put(teamId, new TeamAgg(teamId));
            }
        }
        if (!CollectionUtils.isEmpty(matches)) {
            for (ConfirmedMatchBriefVo match : matches) {
                applyMatch(aggMap, match, rule);
            }
        }
        if (aggMap.isEmpty()) {
            return List.of();
        }
        List<TeamAgg> teams = new ArrayList<>(aggMap.values());
        teams.sort(buildComparator(matches, rule));
        List<BasketStageStanding> rows = new ArrayList<>(teams.size());
        int rank = 1;
        for (TeamAgg team : teams) {
            BasketStageStanding row = new BasketStageStanding();
            row.setSeasonId(seasonId);
            row.setStageId(stageId);
            row.setGroupId(groupId);
            row.setTeamId(team.teamId);
            row.setRankNo(rank++);
            row.setMatchesPlayed(team.played);
            row.setWins(team.wins);
            row.setLosses(team.losses);
            row.setPointsFor(team.pointsFor);
            row.setPointsAgainst(team.pointsAgainst);
            row.setPointDiff(team.pointsFor - team.pointsAgainst);
            row.setStandingPoints(team.standingPoints);
            row.setWinRate(StatMathUtils.winRate(team.wins, team.played));
            row.setStreakType(team.streakType);
            row.setStreakCount(team.streakCount);
            row.setQualifiedFlag(0);
            row.setSnapshotTime(snapshotTime);
            row.setIsDelete(0);
            rows.add(row);
        }
        return rows;
    }

    private void applyMatch(Map<Long, TeamAgg> aggMap, ConfirmedMatchBriefVo match, StandingPointsRule rule) {
        TeamAgg home = aggMap.computeIfAbsent(match.getHomeTeamId(), TeamAgg::new);
        TeamAgg away = aggMap.computeIfAbsent(match.getAwayTeamId(), TeamAgg::new);
        int homeScore = nvl(match.getHomeScore());
        int awayScore = nvl(match.getAwayScore());

        home.played++;
        away.played++;
        home.pointsFor += homeScore;
        home.pointsAgainst += awayScore;
        away.pointsFor += awayScore;
        away.pointsAgainst += homeScore;

        BigDecimal homePts = resolvePoints(match, match.getHomeTeamId(), match.getHomeResultType(), rule);
        BigDecimal awayPts = resolvePoints(match, match.getAwayTeamId(), match.getAwayResultType(), rule);
        home.standingPoints = home.standingPoints.add(homePts);
        away.standingPoints = away.standingPoints.add(awayPts);

        boolean homeWin = isWin(match, match.getHomeTeamId(), match.getHomeResultType());
        boolean awayWin = isWin(match, match.getAwayTeamId(), match.getAwayResultType());
        if (homeWin) {
            home.wins++;
            away.losses++;
            home.updateStreak("W");
            away.updateStreak("L");
        } else if (awayWin) {
            away.wins++;
            home.losses++;
            away.updateStreak("W");
            home.updateStreak("L");
        }
    }

    private BigDecimal resolvePoints(ConfirmedMatchBriefVo match, Long teamId, String resultType,
                                     StandingPointsRule rule) {
        if (resultType != null && !resultType.isBlank()) {
            MatchResultTypeEnum type = MatchResultTypeEnum.fromCode(resultType);
            return switch (type) {
                case WIN -> rule.getWinPoints();
                case LOSS -> rule.getLossPoints();
                case FORFEIT_WIN -> rule.getForfeitWinPoints();
                case FORFEIT_LOSS -> rule.getForfeitLossPoints();
                default -> BigDecimal.ZERO;
            };
        }
        if (match.getWalkoverTeamId() != null) {
            if (Objects.equals(match.getWalkoverTeamId(), teamId)) {
                return rule.getForfeitLossPoints();
            }
            return rule.getForfeitWinPoints();
        }
        if (match.getWinnerTeamId() == null) {
            return BigDecimal.ZERO;
        }
        if (Objects.equals(match.getWinnerTeamId(), teamId)) {
            return rule.getWinPoints();
        }
        return rule.getLossPoints();
    }

    private boolean isWin(ConfirmedMatchBriefVo match, Long teamId, String resultType) {
        if (resultType != null && !resultType.isBlank()) {
            MatchResultTypeEnum type = MatchResultTypeEnum.fromCode(resultType);
            return type == MatchResultTypeEnum.WIN || type == MatchResultTypeEnum.FORFEIT_WIN;
        }
        if (match.getWalkoverTeamId() != null) {
            return !Objects.equals(match.getWalkoverTeamId(), teamId);
        }
        return match.getWinnerTeamId() != null && Objects.equals(match.getWinnerTeamId(), teamId);
    }

    private Comparator<TeamAgg> buildComparator(List<ConfirmedMatchBriefVo> matches, StandingPointsRule rule) {
        List<StandingTieBreakEnum> order = rule.getTieBreakOrder();
        return (a, b) -> {
            for (StandingTieBreakEnum item : order) {
                int cmp = compareTieBreak(a, b, item, matches, rule);
                if (cmp != 0) {
                    return cmp;
                }
            }
            return Long.compare(a.teamId, b.teamId);
        };
    }

    private int compareTieBreak(TeamAgg a, TeamAgg b, StandingTieBreakEnum item,
                                List<ConfirmedMatchBriefVo> matches, StandingPointsRule rule) {
        return switch (item) {
            case STANDING_POINTS -> b.standingPoints.compareTo(a.standingPoints);
            case POINT_DIFF -> Integer.compare(
                    (b.pointsFor - b.pointsAgainst), (a.pointsFor - a.pointsAgainst));
            case POINTS_FOR -> Integer.compare(b.pointsFor, a.pointsFor);
            case POINTS_AGAINST_ASC -> Integer.compare(a.pointsAgainst, b.pointsAgainst);
            case WINS -> Integer.compare(b.wins, a.wins);
            case HEAD_TO_HEAD_POINTS -> compareH2hPoints(a, b, matches, rule);
            case HEAD_TO_HEAD_DIFF -> compareH2hDiff(a, b, matches);
            case TEAM_ID -> Long.compare(a.teamId, b.teamId);
        };
    }

    private int compareH2hPoints(TeamAgg a, TeamAgg b, List<ConfirmedMatchBriefVo> matches,
                                 StandingPointsRule rule) {
        BigDecimal aPts = BigDecimal.ZERO;
        BigDecimal bPts = BigDecimal.ZERO;
        for (ConfirmedMatchBriefVo match : matches) {
            if (!isH2h(match, a.teamId, b.teamId)) {
                continue;
            }
            aPts = aPts.add(resolvePoints(match, a.teamId,
                    Objects.equals(match.getHomeTeamId(), a.teamId) ? match.getHomeResultType() : match.getAwayResultType(),
                    rule));
            bPts = bPts.add(resolvePoints(match, b.teamId,
                    Objects.equals(match.getHomeTeamId(), b.teamId) ? match.getHomeResultType() : match.getAwayResultType(),
                    rule));
        }
        return bPts.compareTo(aPts);
    }

    private int compareH2hDiff(TeamAgg a, TeamAgg b, List<ConfirmedMatchBriefVo> matches) {
        int aDiff = 0;
        int bDiff = 0;
        for (ConfirmedMatchBriefVo match : matches) {
            if (!isH2h(match, a.teamId, b.teamId)) {
                continue;
            }
            int home = nvl(match.getHomeScore());
            int away = nvl(match.getAwayScore());
            if (Objects.equals(match.getHomeTeamId(), a.teamId)) {
                aDiff += home - away;
                bDiff += away - home;
            } else {
                aDiff += away - home;
                bDiff += home - away;
            }
        }
        return Integer.compare(bDiff, aDiff);
    }

    private boolean isH2h(ConfirmedMatchBriefVo match, Long teamA, Long teamB) {
        return (Objects.equals(match.getHomeTeamId(), teamA) && Objects.equals(match.getAwayTeamId(), teamB))
                || (Objects.equals(match.getHomeTeamId(), teamB) && Objects.equals(match.getAwayTeamId(), teamA));
    }

    private BasketTournamentSeason requireSeason(Long seasonId) {
        if (seasonId == null) {
            throw new ServiceException("赛季ID不能为空");
        }
        BasketTournamentSeason season = seasonMapper.selectById(seasonId);
        if (season == null || (season.getIsDelete() != null && season.getIsDelete() != 0)) {
            throw new ServiceException("赛季不存在");
        }
        return season;
    }

    private BasketTournamentStage requireStage(Long stageId) {
        if (stageId == null) {
            throw new ServiceException("阶段ID不能为空");
        }
        BasketTournamentStage stage = stageMapper.selectById(stageId);
        if (stage == null || (stage.getIsDelete() != null && stage.getIsDelete() != 0)) {
            throw new ServiceException("阶段不存在");
        }
        return stage;
    }

    private void requireRule(StandingPointsRule rule) {
        if (rule == null || rule.getWinPoints() == null || rule.getLossPoints() == null) {
            throw new ServiceException("积分规则未配置");
        }
    }

    private int nvl(Integer v) {
        return v == null ? 0 : v;
    }

    private static final class TeamAgg {
        private final Long teamId;
        private int played;
        private int wins;
        private int losses;
        private int pointsFor;
        private int pointsAgainst;
        private BigDecimal standingPoints = BigDecimal.ZERO;
        private String streakType;
        private int streakCount;

        private TeamAgg(Long teamId) {
            this.teamId = teamId;
        }

        private void updateStreak(String type) {
            if (type.equals(streakType)) {
                streakCount++;
            } else {
                streakType = type;
                streakCount = 1;
            }
        }
    }
}
