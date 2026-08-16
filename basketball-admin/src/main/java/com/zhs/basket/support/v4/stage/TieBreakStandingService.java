package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketDivisionTieBreakRule;
import com.zhs.basket.domain.v4.BasketStanding;
import com.zhs.basket.domain.v4.BasketStandingRankReason;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.support.v4.StageFormatSpecification;
import com.zhs.basket.enums.v4.MatchStatusEnum;
import com.zhs.basket.enums.v4.TieBreakRuleEnum;
import com.zhs.basket.mapper.v4.BasketDivisionStageMapper;
import com.zhs.basket.mapper.v4.BasketDivisionTieBreakRuleMapper;
import com.zhs.basket.mapper.v4.BasketStandingMapper;
import com.zhs.basket.mapper.v4.BasketStandingRankReasonMapper;
import com.zhs.basket.domain.BasketPointAdjustment;
import com.zhs.basket.mapper.BasketPointAdjustmentMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 阶段积分榜重建与同分排序。
 */
@Service
public class TieBreakStandingService {

    private static final Integer NOT_DELETED = 0;
    private static final long OVERALL_SCOPE_KEY = 0L;

    @Autowired
    private BasketDivisionStageMapper stageMapper;
    @Autowired
    private BasketDivisionTieBreakRuleMapper tieBreakRuleMapper;
    @Autowired
    private BasketV4MatchMapper matchMapper;
    @Autowired
    private BasketStandingMapper standingMapper;
    @Autowired
    private BasketStandingRankReasonMapper rankReasonMapper;
    @Autowired
    private TieBreakComparatorBuilder comparatorBuilder;
    @Autowired
    private BasketPointAdjustmentMapper pointAdjustmentMapper;

    @Transactional(rollbackFor = Exception.class)
    public void rebuildStageStanding(Long stageId) {
        BasketDivisionStage stage = requireStage(stageId);
        List<BasketV4Match> stageMatches = matchMapper.selectFinishedByStageId(stageId).stream()
                .filter(match -> MatchStatusEnum.FINISHED.getCode().equals(match.getMatchStatus())
                        || MatchStatusEnum.FORFEITED.getCode().equals(match.getMatchStatus()))
                .collect(Collectors.toList());
        if (stageMatches.isEmpty()) {
            clearExistingStanding(stage);
            return;
        }

        int winPoints = defaultPoints(stage.getWinPoints(), 2);
        int lossPoints = defaultPoints(stage.getLossPoints(), 1);
        int forfeitWinPoints = stage.getForfeitWinPoints() != null ? stage.getForfeitWinPoints() : winPoints;
        int forfeitLossPoints = stage.getForfeitLossPoints() != null ? stage.getForfeitLossPoints() : 0;
        List<BasketDivisionTieBreakRule> tieBreakRules = tieBreakRuleMapper.selectByStageId(stageId);
        Long seasonId = stageMatches.get(0).getSeasonId();
        Long divisionId = stage.getDivisionId();
        Date now = new Date();

        Map<Long, List<BasketV4Match>> scopeMatches = groupMatchesByScope(stage, stageMatches);
        for (Map.Entry<Long, List<BasketV4Match>> entry : scopeMatches.entrySet()) {
            Long groupScopeKey = entry.getKey();
            List<BasketV4Match> matches = entry.getValue();
            Map<Long, StandingAgg> aggMap = aggregateMatches(matches, winPoints, lossPoints, forfeitWinPoints, forfeitLossPoints);
            applyV4PointAdjustments(aggMap, divisionId, stageId);
            List<StandingAgg> sorted = comparatorBuilder.sortStandings(new ArrayList<>(aggMap.values()), tieBreakRules, matches);
            Set<Long> manualPendingTeams = comparatorBuilder.findStillTiedTeamIds(sorted, tieBreakRules, matches);
            boolean manualRuleEnabled = comparatorBuilder.hasManualRule(tieBreakRules);
            Map<Long, BasketStanding> previousManual = loadPreviousManualRanks(divisionId, stageId, groupScopeKey);

            rankReasonMapper.softDeleteByStageScope(divisionId, stageId, groupScopeKey);
            standingMapper.deleteByStageScope(divisionId, stageId, groupScopeKey);
            int rank = 1;
            for (int i = 0; i < sorted.size(); i++) {
                StandingAgg agg = sorted.get(i);
                StandingAgg prev = i > 0 ? sorted.get(i - 1) : null;
                StandingAgg next = i + 1 < sorted.size() ? sorted.get(i + 1) : null;
                StandingAgg rankPeer = next != null ? next : prev;
                List<StandingAgg> tiedGroup = comparatorBuilder.findTiedGroup(sorted, agg, tieBreakRules, matches);
                BasketStanding row = new BasketStanding();
                row.setSeasonId(seasonId);
                row.setDivisionId(divisionId);
                row.setStageId(stageId);
                row.setGroupId(groupScopeKey == OVERALL_SCOPE_KEY ? null : groupScopeKey);
                row.setGroupScopeKey(groupScopeKey);
                row.setEventTeamId(agg.eventTeamId);
                row.setRankNo(rank++);
                row.setWinCount(agg.winCount);
                row.setLoseCount(agg.loseCount);
                row.setDrawCount(0);
                row.setPointFor(agg.pointFor);
                row.setPointAgainst(agg.pointAgainst);
                row.setPointDiff(agg.pointFor - agg.pointAgainst);
                row.setStandingPoints(agg.standingPoints);
                row.setManualRankPending(manualRuleEnabled && manualPendingTeams.contains(agg.eventTeamId) ? 1 : 0);
                restoreManualRank(row, previousManual.get(agg.eventTeamId));
                row.setActiveUniqueKey(buildActiveUniqueKey(divisionId, stageId, groupScopeKey, agg.eventTeamId));
                row.setCreateTime(now);
                row.setUpdateTime(now);
                row.setIsDelete(NOT_DELETED);
                standingMapper.insert(row);

                List<BasketStandingRankReason> reasons = buildRankReasons(
                        divisionId, stageId, groupScopeKey, row, agg, rankPeer, tiedGroup, tieBreakRules,
                        manualPendingTeams.contains(agg.eventTeamId), matches, now);
                if (!reasons.isEmpty()) {
                    rankReasonMapper.batchInsert(reasons);
                }
            }
        }
    }

    public boolean hasManualRankPending(Long stageId) {
        return standingMapper.countManualPendingByStageId(stageId) > 0;
    }

    private List<BasketStandingRankReason> buildRankReasons(Long divisionId,
                                                            Long stageId,
                                                            Long groupScopeKey,
                                                            BasketStanding standing,
                                                            StandingAgg agg,
                                                            StandingAgg rankPeer,
                                                            List<StandingAgg> tiedGroup,
                                                            List<BasketDivisionTieBreakRule> rules,
                                                            boolean manualPending,
                                                            List<BasketV4Match> scopeMatches,
                                                            Date now) {
        List<BasketStandingRankReason> result = new ArrayList<>();
        List<BasketDivisionTieBreakRule> enabledRules = sortedEnabledRules(rules);
        String decisiveRule = comparatorBuilder.findDecisiveRuleCode(agg, rankPeer, tiedGroup, enabledRules, scopeMatches);
        for (BasketDivisionTieBreakRule rule : enabledRules) {
            TieBreakRuleEnum ruleEnum = TieBreakRuleEnum.fromCode(rule.getRuleCode());
            if (ruleEnum == TieBreakRuleEnum.MANUAL) {
                if (manualPending) {
                    result.add(buildReasonRow(divisionId, stageId, groupScopeKey, standing, agg.eventTeamId,
                            rule, "待人工裁定", manualPending ? 1 : 0, now));
                }
                continue;
            }
            String compareValue = comparatorBuilder.compareValueFor(ruleEnum, agg, tiedGroup, scopeMatches);
            String reasonText = ruleEnum.getDescription() + "：" + compareValue;
            int decisive = rule.getRuleCode().equals(decisiveRule) ? 1 : 0;
            result.add(buildReasonRow(divisionId, stageId, groupScopeKey, standing, agg.eventTeamId,
                    rule, compareValue, reasonText, decisive, now));
        }
        return result;
    }

    private BasketStandingRankReason buildReasonRow(Long divisionId,
                                                    Long stageId,
                                                    Long groupScopeKey,
                                                    BasketStanding standing,
                                                    Long eventTeamId,
                                                    BasketDivisionTieBreakRule rule,
                                                    String compareValue,
                                                    String reasonText,
                                                    int decisive,
                                                    Date now) {
        BasketStandingRankReason reason = new BasketStandingRankReason();
        reason.setDivisionId(divisionId);
        reason.setStageId(stageId);
        reason.setGroupId(groupScopeKey == OVERALL_SCOPE_KEY ? null : groupScopeKey);
        reason.setStandingId(standing.getId());
        reason.setEventTeamId(eventTeamId);
        reason.setRuleCode(rule.getRuleCode());
        reason.setPriorityNo(rule.getPriorityNo());
        reason.setCompareValue(compareValue);
        reason.setReasonText(reasonText);
        reason.setIsDecisive(decisive);
        reason.setCreateTime(now);
        reason.setUpdateTime(now);
        reason.setIsDelete(NOT_DELETED);
        return reason;
    }

    private BasketStandingRankReason buildReasonRow(Long divisionId,
                                                    Long stageId,
                                                    Long groupScopeKey,
                                                    BasketStanding standing,
                                                    Long eventTeamId,
                                                    BasketDivisionTieBreakRule rule,
                                                    String reasonText,
                                                    int decisive,
                                                    Date now) {
        return buildReasonRow(divisionId, stageId, groupScopeKey, standing, eventTeamId,
                rule, null, reasonText, decisive, now);
    }

    private List<BasketDivisionTieBreakRule> sortedEnabledRules(List<BasketDivisionTieBreakRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return List.of();
        }
        return rules.stream()
                .filter(rule -> rule.getEnabled() == null || Integer.valueOf(1).equals(rule.getEnabled()))
                .sorted(Comparator.comparing(BasketDivisionTieBreakRule::getPriorityNo,
                        Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    private Map<Long, List<BasketV4Match>> groupMatchesByScope(BasketDivisionStage stage, List<BasketV4Match> matches) {
        Map<Long, List<BasketV4Match>> scopes = new LinkedHashMap<>();
        if (isGroupStage(stage)) {
            for (BasketV4Match match : matches) {
                Long groupId = match.getGroupId();
                if (groupId == null) {
                    continue;
                }
                scopes.computeIfAbsent(groupId, key -> new ArrayList<>()).add(match);
            }
        } else {
            scopes.put(OVERALL_SCOPE_KEY, matches);
        }
        return scopes;
    }

    private Map<Long, StandingAgg> aggregateMatches(List<BasketV4Match> matches, int winPoints, int lossPoints,
                                                    int forfeitWinPoints, int forfeitLossPoints) {
        Map<Long, StandingAgg> aggMap = new LinkedHashMap<>();
        for (BasketV4Match match : matches) {
            if (MatchStatusEnum.FORFEITED.getCode().equals(match.getMatchStatus())) {
                applyForfeit(aggMap, match, forfeitWinPoints, forfeitLossPoints);
                continue;
            }
            if (match.getHomeScore() == null || match.getAwayScore() == null) {
                continue;
            }
            StandingAgg home = aggMap.computeIfAbsent(match.getHomeEventTeamId(), StandingAgg::new);
            StandingAgg away = aggMap.computeIfAbsent(match.getAwayEventTeamId(), StandingAgg::new);
            home.pointFor += match.getHomeScore();
            home.pointAgainst += match.getAwayScore();
            away.pointFor += match.getAwayScore();
            away.pointAgainst += match.getHomeScore();
            Long winnerId = match.getWinnerEventTeamId();
            if (winnerId != null && winnerId.equals(match.getHomeEventTeamId())) {
                home.winCount++;
                home.standingPoints += winPoints;
                away.loseCount++;
                away.standingPoints += lossPoints;
            } else if (winnerId != null && winnerId.equals(match.getAwayEventTeamId())) {
                away.winCount++;
                away.standingPoints += winPoints;
                home.loseCount++;
                home.standingPoints += lossPoints;
            }
        }
        return aggMap;
    }

    private void clearExistingStanding(BasketDivisionStage stage) {
        List<BasketStanding> existing = standingMapper.selectByStageId(stage.getId());
        if (existing == null || existing.isEmpty()) {
            return;
        }
        Set<Long> scopes = new HashSet<>();
        for (BasketStanding row : existing) {
            scopes.add(row.getGroupScopeKey() != null ? row.getGroupScopeKey() : OVERALL_SCOPE_KEY);
        }
        for (Long scope : scopes) {
            rankReasonMapper.softDeleteByStageScope(stage.getDivisionId(), stage.getId(), scope);
            standingMapper.deleteByStageScope(stage.getDivisionId(), stage.getId(), scope);
        }
    }

    private void applyV4PointAdjustments(Map<Long, StandingAgg> aggMap, Long divisionId, Long stageId) {
        if (aggMap == null || aggMap.isEmpty() || divisionId == null || stageId == null) {
            return;
        }
        List<BasketPointAdjustment> sums = pointAdjustmentMapper.selectV4SumByStage(divisionId, stageId);
        if (sums == null || sums.isEmpty()) {
            return;
        }
        for (BasketPointAdjustment sum : sums) {
            if (sum.getEventTeamId() == null || sum.getAdjustmentPoints() == null) {
                continue;
            }
            StandingAgg agg = aggMap.get(sum.getEventTeamId());
            if (agg == null) {
                continue;
            }
            agg.standingPoints += sum.getAdjustmentPoints();
        }
    }

    private void applyForfeit(Map<Long, StandingAgg> aggMap, BasketV4Match match,
                              int forfeitWinPoints, int forfeitLossPoints) {
        Long winnerId = match.getWinnerEventTeamId();
        Long loserId = match.getLoserEventTeamId();
        if (winnerId == null || loserId == null) {
            return;
        }
        StandingAgg winner = aggMap.computeIfAbsent(winnerId, StandingAgg::new);
        StandingAgg loser = aggMap.computeIfAbsent(loserId, StandingAgg::new);
        winner.winCount++;
        winner.standingPoints += forfeitWinPoints;
        loser.loseCount++;
        loser.standingPoints += forfeitLossPoints;
    }

    private Map<Long, BasketStanding> loadPreviousManualRanks(Long divisionId, Long stageId, Long groupScopeKey) {
        List<BasketStanding> previous = standingMapper.selectByStageScope(divisionId, stageId, groupScopeKey);
        Map<Long, BasketStanding> result = new HashMap<>();
        if (previous == null) {
            return result;
        }
        for (BasketStanding row : previous) {
            if (row.getEventTeamId() != null && row.getManualRankNo() != null) {
                result.put(row.getEventTeamId(), row);
            }
        }
        return result;
    }

    private void restoreManualRank(BasketStanding row, BasketStanding previous) {
        if (previous == null) {
            return;
        }
        row.setManualRankNo(previous.getManualRankNo());
        row.setManualRankReason(previous.getManualRankReason());
        row.setManualRankBy(previous.getManualRankBy());
        row.setManualRankTime(previous.getManualRankTime());
    }

    private boolean isGroupStage(BasketDivisionStage stage) {
        return StageFormatSpecification.isGrouped(stage);
    }

    private int defaultPoints(Integer configured, int fallback) {
        return configured != null ? configured : fallback;
    }

    public static String buildActiveUniqueKey(Long divisionId, Long stageId, Long groupScopeKey, Long eventTeamId) {
        String groupPart = groupScopeKey == null || groupScopeKey == OVERALL_SCOPE_KEY
                ? "GLOBAL" : String.valueOf(groupScopeKey);
        return "DIVISION:" + divisionId + ":STAGE:" + stageId + ":GROUP:" + groupPart + ":TEAM:" + eventTeamId;
    }

    private BasketDivisionStage requireStage(Long stageId) {
        if (stageId == null) {
            throw new ServiceException("stageId 不能为空");
        }
        BasketDivisionStage stage = stageMapper.selectById(stageId);
        if (stage == null) {
            throw new ServiceException("阶段不存在");
        }
        return stage;
    }

    public static class StandingAgg {
        private final Long eventTeamId;
        private int winCount;
        private int loseCount;
        private int pointFor;
        private int pointAgainst;
        private int standingPoints;

        StandingAgg(Long eventTeamId) {
            this.eventTeamId = eventTeamId;
        }

        public Long getEventTeamId() {
            return eventTeamId;
        }

        public int getWinCount() {
            return winCount;
        }

        public int getPointDiff() {
            return pointFor - pointAgainst;
        }

        public int getPointFor() {
            return pointFor;
        }

        public int getStandingPoints() {
            return standingPoints;
        }
    }
}
