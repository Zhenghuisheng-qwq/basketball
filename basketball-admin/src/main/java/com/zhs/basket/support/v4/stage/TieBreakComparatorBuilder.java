package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivisionTieBreakRule;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.enums.v4.TieBreakRuleEnum;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 按阶段同分规则 priority_no 构建比较器链；多队同分时在 mini-table 内比较相互战绩。
 */
@Component
public class TieBreakComparatorBuilder {

    public List<TieBreakStandingService.StandingAgg> sortStandings(List<TieBreakStandingService.StandingAgg> standings,
                                                                   List<BasketDivisionTieBreakRule> rules,
                                                                   List<BasketV4Match> scopeMatches) {
        if (standings == null || standings.isEmpty()) {
            return List.of();
        }
        List<TieBreakStandingService.StandingAgg> byPoints = new ArrayList<>(standings);
        byPoints.sort(Comparator.comparingInt(TieBreakStandingService.StandingAgg::getStandingPoints).reversed());

        List<TieBreakStandingService.StandingAgg> result = new ArrayList<>();
        int start = 0;
        while (start < byPoints.size()) {
            int end = start + 1;
            while (end < byPoints.size()
                    && byPoints.get(end).getStandingPoints() == byPoints.get(start).getStandingPoints()) {
                end++;
            }
            List<TieBreakStandingService.StandingAgg> tieGroup = new ArrayList<>(byPoints.subList(start, end));
            if (tieGroup.size() > 1) {
                sortTiedGroup(tieGroup, rules, scopeMatches);
            }
            result.addAll(tieGroup);
            start = end;
        }
        return result;
    }

    public void sortTiedGroup(List<TieBreakStandingService.StandingAgg> group,
                              List<BasketDivisionTieBreakRule> rules,
                              List<BasketV4Match> scopeMatches) {
        Comparator<TieBreakStandingService.StandingAgg> comparator = buildTiedGroupComparator(group, rules, scopeMatches);
        if (comparator != null) {
            group.sort(comparator);
        }
    }

    public List<TieBreakStandingService.StandingAgg> findTiedGroup(List<TieBreakStandingService.StandingAgg> sorted,
                                                                   TieBreakStandingService.StandingAgg target,
                                                                   List<BasketDivisionTieBreakRule> rules,
                                                                   List<BasketV4Match> scopeMatches) {
        if (sorted == null || target == null) {
            return List.of();
        }
        List<TieBreakStandingService.StandingAgg> samePoints = sorted.stream()
                .filter(item -> item.getStandingPoints() == target.getStandingPoints())
                .collect(Collectors.toCollection(ArrayList::new));
        if (samePoints.size() <= 1) {
            return samePoints;
        }
        List<TieBreakStandingService.StandingAgg> copy = new ArrayList<>(samePoints);
        sortTiedGroup(copy, rules, scopeMatches);
        Comparator<TieBreakStandingService.StandingAgg> comparator = buildTiedGroupComparator(copy, rules, scopeMatches);
        if (comparator == null) {
            return copy;
        }
        List<TieBreakStandingService.StandingAgg> tied = new ArrayList<>();
        tied.add(target);
        for (TieBreakStandingService.StandingAgg candidate : copy) {
            if (candidate.getEventTeamId().equals(target.getEventTeamId())) {
                continue;
            }
            if (comparator.compare(target, candidate) == 0) {
                tied.add(candidate);
            }
        }
        return tied;
    }

    public boolean hasManualRule(List<BasketDivisionTieBreakRule> rules) {
        return sortedEnabledRules(rules).stream()
                .anyMatch(rule -> TieBreakRuleEnum.MANUAL.getCode().equals(rule.getRuleCode()));
    }

    public boolean isStillTied(TieBreakStandingService.StandingAgg left,
                               TieBreakStandingService.StandingAgg right,
                               List<BasketDivisionTieBreakRule> rules,
                               List<BasketV4Match> scopeMatches) {
        if (left == null || right == null) {
            return false;
        }
        List<TieBreakStandingService.StandingAgg> group = List.of(left, right);
        Comparator<TieBreakStandingService.StandingAgg> comparator = buildTiedGroupComparator(group, rules, scopeMatches);
        return comparator != null && comparator.compare(left, right) == 0;
    }

    public Set<Long> findStillTiedTeamIds(List<TieBreakStandingService.StandingAgg> sorted,
                                          List<BasketDivisionTieBreakRule> rules,
                                          List<BasketV4Match> scopeMatches) {
        Set<Long> tied = new HashSet<>();
        if (sorted == null || sorted.size() < 2) {
            return tied;
        }
        int index = 0;
        while (index < sorted.size()) {
            int end = index + 1;
            while (end < sorted.size()
                    && sorted.get(end).getStandingPoints() == sorted.get(index).getStandingPoints()) {
                end++;
            }
            if (end - index > 1) {
                List<TieBreakStandingService.StandingAgg> group = new ArrayList<>(sorted.subList(index, end));
                Comparator<TieBreakStandingService.StandingAgg> comparator =
                        buildTiedGroupComparator(group, rules, scopeMatches);
                if (comparator == null) {
                    index = end;
                    continue;
                }
                int inner = index;
                while (inner < end) {
                    int innerEnd = inner + 1;
                    while (innerEnd < end && comparator.compare(sorted.get(inner), sorted.get(innerEnd)) == 0) {
                        innerEnd++;
                    }
                    if (innerEnd - inner > 1) {
                        for (int i = inner; i < innerEnd; i++) {
                            tied.add(sorted.get(i).getEventTeamId());
                        }
                    }
                    inner = innerEnd;
                }
            }
            index = end;
        }
        return tied;
    }

    public String compareValueFor(TieBreakRuleEnum ruleEnum,
                                  TieBreakStandingService.StandingAgg agg,
                                  List<TieBreakStandingService.StandingAgg> tiedGroup,
                                  List<BasketV4Match> scopeMatches) {
        return switch (ruleEnum) {
            case WIN_COUNT -> String.valueOf(agg.getWinCount());
            case POINT_DIFFERENCE -> String.valueOf(agg.getPointDiff());
            case TOTAL_POINTS -> String.valueOf(agg.getPointFor());
            case HEAD_TO_HEAD -> String.valueOf(miniTableWins(agg.getEventTeamId(), tiedGroup, scopeMatches));
            case MANUAL -> "PENDING";
        };
    }

    public String findDecisiveRuleCode(TieBreakStandingService.StandingAgg current,
                                       TieBreakStandingService.StandingAgg peer,
                                       List<TieBreakStandingService.StandingAgg> tiedGroup,
                                       List<BasketDivisionTieBreakRule> rules,
                                       List<BasketV4Match> scopeMatches) {
        if (peer == null) {
            List<BasketDivisionTieBreakRule> enabled = sortedEnabledRules(rules);
            return enabled.isEmpty() ? null : enabled.get(0).getRuleCode();
        }
        for (BasketDivisionTieBreakRule rule : sortedEnabledRules(rules)) {
            TieBreakRuleEnum ruleEnum = TieBreakRuleEnum.fromCode(rule.getRuleCode());
            if (ruleEnum == TieBreakRuleEnum.MANUAL) {
                continue;
            }
            String currentValue = compareValueFor(ruleEnum, current, tiedGroup, scopeMatches);
            String peerValue = compareValueFor(ruleEnum, peer, tiedGroup, scopeMatches);
            if (!currentValue.equals(peerValue)) {
                return rule.getRuleCode();
            }
        }
        return null;
    }

    private Comparator<TieBreakStandingService.StandingAgg> buildTiedGroupComparator(
            List<TieBreakStandingService.StandingAgg> tiedGroup,
            List<BasketDivisionTieBreakRule> rules,
            List<BasketV4Match> scopeMatches) {
        Comparator<TieBreakStandingService.StandingAgg> comparator = null;
        for (BasketDivisionTieBreakRule rule : sortedEnabledRules(rules)) {
            TieBreakRuleEnum ruleEnum = TieBreakRuleEnum.fromCode(rule.getRuleCode());
            Comparator<TieBreakStandingService.StandingAgg> next = comparatorForGroup(ruleEnum, tiedGroup, scopeMatches);
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
        return comparator;
    }

    private Comparator<TieBreakStandingService.StandingAgg> comparatorForGroup(TieBreakRuleEnum ruleEnum,
                                                                               List<TieBreakStandingService.StandingAgg> tiedGroup,
                                                                               List<BasketV4Match> scopeMatches) {
        return switch (ruleEnum) {
            case WIN_COUNT -> Comparator.comparingInt(TieBreakStandingService.StandingAgg::getWinCount).reversed();
            case POINT_DIFFERENCE ->
                    Comparator.comparingInt(TieBreakStandingService.StandingAgg::getPointDiff).reversed();
            case TOTAL_POINTS -> Comparator.comparingInt(TieBreakStandingService.StandingAgg::getPointFor).reversed();
            case HEAD_TO_HEAD -> headToHeadMiniTableComparator(tiedGroup, scopeMatches);
            case MANUAL -> null;
        };
    }

    private Comparator<TieBreakStandingService.StandingAgg> headToHeadMiniTableComparator(
            List<TieBreakStandingService.StandingAgg> tiedGroup,
            List<BasketV4Match> scopeMatches) {
        return (left, right) -> Integer.compare(
                miniTableWins(right.getEventTeamId(), tiedGroup, scopeMatches),
                miniTableWins(left.getEventTeamId(), tiedGroup, scopeMatches));
    }

    int miniTableWins(Long teamId,
                      List<TieBreakStandingService.StandingAgg> tiedGroup,
                      List<BasketV4Match> matches) {
        if (teamId == null || tiedGroup == null || tiedGroup.size() < 2 || matches == null) {
            return 0;
        }
        Set<Long> opponentIds = tiedGroup.stream()
                .map(TieBreakStandingService.StandingAgg::getEventTeamId)
                .filter(id -> id != null && !id.equals(teamId))
                .collect(Collectors.toSet());
        if (opponentIds.isEmpty()) {
            return 0;
        }
        int wins = 0;
        for (BasketV4Match match : matches) {
            if (match.getHomeScore() == null || match.getAwayScore() == null) {
                continue;
            }
            if (match.getHomeScore().equals(match.getAwayScore())) {
                continue;
            }
            Long home = match.getHomeEventTeamId();
            Long away = match.getAwayEventTeamId();
            if (!teamId.equals(home) && !teamId.equals(away)) {
                continue;
            }
            Long opponentId = teamId.equals(home) ? away : home;
            if (!opponentIds.contains(opponentId)) {
                continue;
            }
            Long winner = match.getHomeScore() > match.getAwayScore() ? home : away;
            if (teamId.equals(winner)) {
                wins++;
            }
        }
        return wins;
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
}
