package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivisionTieBreakRule;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.enums.v4.TieBreakRuleEnum;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 按阶段同分规则 priority_no 构建比较器链。
 */
@Component
public class TieBreakComparatorBuilder {

    public Comparator<TieBreakStandingService.StandingAgg> build(List<BasketDivisionTieBreakRule> rules,
                                                                   List<BasketV4Match> scopeMatches) {
        Comparator<TieBreakStandingService.StandingAgg> comparator = null;
        for (BasketDivisionTieBreakRule rule : sortedEnabledRules(rules)) {
            TieBreakRuleEnum ruleEnum = TieBreakRuleEnum.fromCode(rule.getRuleCode());
            Comparator<TieBreakStandingService.StandingAgg> next = comparatorFor(ruleEnum, scopeMatches);
            if (next == null) {
                continue;
            }
            comparator = comparator == null ? next : comparator.thenComparing(next);
        }
        if (comparator == null) {
            comparator = Comparator.comparingInt(TieBreakStandingService.StandingAgg::getStandingPoints).reversed()
                    .thenComparing(Comparator.comparingInt(TieBreakStandingService.StandingAgg::getWinCount).reversed())
                    .thenComparing(Comparator.comparingInt(TieBreakStandingService.StandingAgg::getPointDiff).reversed())
                    .thenComparing(Comparator.comparingInt(TieBreakStandingService.StandingAgg::getPointFor).reversed());
        }
        return comparator.thenComparing(TieBreakStandingService.StandingAgg::getEventTeamId);
    }

    public boolean hasManualRule(List<BasketDivisionTieBreakRule> rules) {
        return sortedEnabledRules(rules).stream()
                .anyMatch(rule -> TieBreakRuleEnum.MANUAL.getCode().equals(rule.getRuleCode()));
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

    private Comparator<TieBreakStandingService.StandingAgg> comparatorFor(TieBreakRuleEnum ruleEnum,
                                                                          List<BasketV4Match> scopeMatches) {
        return switch (ruleEnum) {
            case WIN_COUNT -> Comparator.comparingInt(TieBreakStandingService.StandingAgg::getWinCount).reversed();
            case POINT_DIFFERENCE ->
                    Comparator.comparingInt(TieBreakStandingService.StandingAgg::getPointDiff).reversed();
            case TOTAL_POINTS -> Comparator.comparingInt(TieBreakStandingService.StandingAgg::getPointFor).reversed();
            case HEAD_TO_HEAD -> headToHeadComparator(scopeMatches);
            case MANUAL -> null;
        };
    }

    private Comparator<TieBreakStandingService.StandingAgg> headToHeadComparator(List<BasketV4Match> scopeMatches) {
        Map<Long, Map<Long, Integer>> winMatrix = buildHeadToHeadWinMatrix(scopeMatches);
        return (a, b) -> {
            int aWins = winMatrix.getOrDefault(a.getEventTeamId(), Map.of())
                    .getOrDefault(b.getEventTeamId(), 0);
            int bWins = winMatrix.getOrDefault(b.getEventTeamId(), Map.of())
                    .getOrDefault(a.getEventTeamId(), 0);
            return Integer.compare(bWins, aWins);
        };
    }

    private Map<Long, Map<Long, Integer>> buildHeadToHeadWinMatrix(List<BasketV4Match> matches) {
        Map<Long, Map<Long, Integer>> matrix = new HashMap<>();
        if (matches == null) {
            return matrix;
        }
        for (BasketV4Match match : matches) {
            if (match.getHomeScore() == null || match.getAwayScore() == null) {
                continue;
            }
            if (match.getHomeScore().equals(match.getAwayScore())) {
                continue;
            }
            Long winnerId = match.getHomeScore() > match.getAwayScore()
                    ? match.getHomeEventTeamId() : match.getAwayEventTeamId();
            Long loserId = match.getHomeScore() > match.getAwayScore()
                    ? match.getAwayEventTeamId() : match.getHomeEventTeamId();
            matrix.computeIfAbsent(winnerId, key -> new HashMap<>())
                    .merge(loserId, 1, Integer::sum);
        }
        return matrix;
    }

    public Set<Long> findStillTiedTeamIds(List<TieBreakStandingService.StandingAgg> sorted,
                                          List<BasketDivisionTieBreakRule> rules,
                                          List<BasketV4Match> scopeMatches) {
        Set<Long> tied = new HashSet<>();
        if (sorted == null || sorted.size() < 2) {
            return tied;
        }
        Comparator<TieBreakStandingService.StandingAgg> comparator = build(rules, scopeMatches);
        int index = 0;
        while (index < sorted.size()) {
            int end = index + 1;
            while (end < sorted.size() && comparator.compare(sorted.get(index), sorted.get(end)) == 0) {
                end++;
            }
            if (end - index > 1) {
                for (int i = index; i < end; i++) {
                    tied.add(sorted.get(i).getEventTeamId());
                }
            }
            index = end;
        }
        return tied;
    }
}
