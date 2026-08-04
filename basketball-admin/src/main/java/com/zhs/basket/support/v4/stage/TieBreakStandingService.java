package com.zhs.basket.support.v4.stage;

import com.alibaba.fastjson2.JSON;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketDivisionTieBreakRule;
import com.zhs.basket.domain.v4.BasketStanding;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.enums.v4.GroupingModeEnum;
import com.zhs.basket.enums.v4.MatchStatusEnum;
import com.zhs.basket.enums.v4.StageTypeEnum;
import com.zhs.basket.mapper.v4.BasketDivisionStageMapper;
import com.zhs.basket.mapper.v4.BasketDivisionTieBreakRuleMapper;
import com.zhs.basket.mapper.v4.BasketStandingMapper;
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
    private TieBreakComparatorBuilder comparatorBuilder;

    @Transactional(rollbackFor = Exception.class)
    public void rebuildStageStanding(Long stageId) {
        BasketDivisionStage stage = requireStage(stageId);
        List<BasketV4Match> stageMatches = matchMapper.selectFinishedByStageId(stageId).stream()
                .filter(match -> MatchStatusEnum.FINISHED.getCode().equals(match.getMatchStatus()))
                .collect(Collectors.toList());
        if (stageMatches.isEmpty()) {
            return;
        }

        int winPoints = defaultPoints(stage.getWinPoints(), 2);
        int lossPoints = defaultPoints(stage.getLossPoints(), 1);
        List<BasketDivisionTieBreakRule> tieBreakRules = tieBreakRuleMapper.selectByStageId(stageId);
        Long seasonId = stageMatches.get(0).getSeasonId();
        Long divisionId = stage.getDivisionId();
        Date now = new Date();

        Map<Long, List<BasketV4Match>> scopeMatches = groupMatchesByScope(stage, stageMatches);
        for (Map.Entry<Long, List<BasketV4Match>> entry : scopeMatches.entrySet()) {
            Long groupScopeKey = entry.getKey();
            List<BasketV4Match> matches = entry.getValue();
            Map<Long, StandingAgg> aggMap = aggregateMatches(matches, winPoints, lossPoints);
            List<StandingAgg> sorted = sortStandings(new ArrayList<>(aggMap.values()), tieBreakRules, matches);
            Set<Long> manualPendingTeams = comparatorBuilder.findStillTiedTeamIds(sorted, tieBreakRules, matches);
            boolean manualRuleEnabled = comparatorBuilder.hasManualRule(tieBreakRules);

            standingMapper.deleteByStageScope(divisionId, stageId, groupScopeKey);
            int rank = 1;
            for (StandingAgg agg : sorted) {
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
                row.setRankExplainJson(buildRankExplainJson(tieBreakRules, manualPendingTeams.contains(agg.eventTeamId)));
                row.setManualRankPending(manualRuleEnabled && manualPendingTeams.contains(agg.eventTeamId) ? 1 : 0);
                row.setActiveUniqueKey(buildActiveUniqueKey(stageId, groupScopeKey, agg.eventTeamId));
                row.setCreateTime(now);
                row.setUpdateTime(now);
                row.setIsDelete(NOT_DELETED);
                standingMapper.insert(row);
            }
        }
    }

    public boolean hasManualRankPending(Long stageId) {
        return standingMapper.countManualPendingByStageId(stageId) > 0;
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

    private Map<Long, StandingAgg> aggregateMatches(List<BasketV4Match> matches, int winPoints, int lossPoints) {
        Map<Long, StandingAgg> aggMap = new LinkedHashMap<>();
        for (BasketV4Match match : matches) {
            if (match.getHomeScore() == null || match.getAwayScore() == null) {
                continue;
            }
            StandingAgg home = aggMap.computeIfAbsent(match.getHomeEventTeamId(), StandingAgg::new);
            StandingAgg away = aggMap.computeIfAbsent(match.getAwayEventTeamId(), StandingAgg::new);
            home.pointFor += match.getHomeScore();
            home.pointAgainst += match.getAwayScore();
            away.pointFor += match.getAwayScore();
            away.pointAgainst += match.getHomeScore();
            if (match.getHomeScore() > match.getAwayScore()) {
                home.winCount++;
                home.standingPoints += winPoints;
                away.loseCount++;
                away.standingPoints += lossPoints;
            } else if (match.getAwayScore() > match.getHomeScore()) {
                away.winCount++;
                away.standingPoints += winPoints;
                home.loseCount++;
                home.standingPoints += lossPoints;
            }
        }
        return aggMap;
    }

    private List<StandingAgg> sortStandings(List<StandingAgg> standings,
                                            List<BasketDivisionTieBreakRule> tieBreakRules,
                                            List<BasketV4Match> scopeMatches) {
        Comparator<StandingAgg> comparator = comparatorBuilder.build(tieBreakRules, scopeMatches);
        standings.sort(comparator);
        return standings;
    }

    private String buildRankExplainJson(List<BasketDivisionTieBreakRule> rules, boolean manualPending) {
        Map<String, Object> explain = new HashMap<>();
        List<String> appliedRules = rules == null ? List.of() : rules.stream()
                .filter(rule -> rule.getEnabled() == null || Integer.valueOf(1).equals(rule.getEnabled()))
                .sorted(Comparator.comparing(BasketDivisionTieBreakRule::getPriorityNo,
                        Comparator.nullsLast(Integer::compareTo)))
                .map(BasketDivisionTieBreakRule::getRuleCode)
                .collect(Collectors.toList());
        explain.put("rules", appliedRules);
        explain.put("manualPending", manualPending);
        return JSON.toJSONString(explain);
    }

    private boolean isGroupStage(BasketDivisionStage stage) {
        if (StageTypeEnum.GROUP.getCode().equals(stage.getStageType())) {
            return true;
        }
        String groupingMode = stage.getGroupingMode();
        return groupingMode != null && !GroupingModeEnum.NONE.getCode().equals(groupingMode);
    }

    private int defaultPoints(Integer configured, int fallback) {
        return configured != null ? configured : fallback;
    }

    public static String buildActiveUniqueKey(Long stageId, Long groupScopeKey, Long eventTeamId) {
        return stageId + "_" + groupScopeKey + "_" + eventTeamId;
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
