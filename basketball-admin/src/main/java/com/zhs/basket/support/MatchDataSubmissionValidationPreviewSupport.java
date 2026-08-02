package com.zhs.basket.support;

import com.zhs.basket.domain.BasketMatch;
import com.zhs.basket.domain.BasketMatchDataSubmission;
import com.zhs.basket.domain.BasketMatchDataSubmissionPeriod;
import com.zhs.basket.domain.BasketMatchDataSubmissionPlayerStat;
import com.zhs.basket.domain.BasketMatchDataSubmissionTeamStat;
import com.zhs.basket.domain.BasketTournamentSeasonRoster;
import com.zhs.basket.enums.MatchPeriodTypeEnum;
import com.zhs.basket.mapper.BasketTournamentSeasonRosterMapper;
import com.zhs.basket.vo.MatchDataSubmissionValidationItemVo;
import com.zhs.basket.vo.MatchDataSubmissionValidationSummaryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class MatchDataSubmissionValidationPreviewSupport {

    @Autowired
    private BasketTournamentSeasonRosterMapper seasonRosterMapper;

    public MatchDataSubmissionValidationSummaryVo preview(BasketMatchDataSubmission submission, BasketMatch match,
            List<BasketMatchDataSubmissionPeriod> periods,
            List<BasketMatchDataSubmissionTeamStat> teamStats,
            List<BasketMatchDataSubmissionPlayerStat> playerStats) {
        List<MatchDataSubmissionValidationItemVo> items = new ArrayList<>();
        if (submission == null) {
            addItem(items, "ERROR", "提交记录不存在", false);
            return summarize(items);
        }
        int homeScore = nvl(submission.getHomeScore());
        int awayScore = nvl(submission.getAwayScore());
        if (homeScore < 0 || awayScore < 0) {
            addItem(items, "ERROR", "比分不能为负数", false);
        } else {
            addItem(items, "OK", "最终比分非负", true);
        }

        validateSubmitTeam(submission, match, items);
        Long submitEventTeamId = resolveSubmitEventTeamId(submission, match);
        validatePeriods(submission, periods, homeScore, awayScore, items);
        validateTeamStats(submission, teamStats, homeScore, awayScore, items);
        validatePlayerStats(submission, playerStats, teamStats, submitEventTeamId, items);
        return summarize(items);
    }

    private Long resolveSubmitEventTeamId(BasketMatchDataSubmission submission, BasketMatch match) {
        if (match == null || submission.getSubmitterTeamId() == null) {
            return null;
        }
        if (Objects.equals(submission.getSubmitterTeamId(), submission.getHomeTeamId())) {
            return match.getHomeEventTeamId();
        }
        if (Objects.equals(submission.getSubmitterTeamId(), submission.getAwayTeamId())) {
            return match.getAwayEventTeamId();
        }
        return null;
    }

    private void validateSubmitTeam(BasketMatchDataSubmission submission, BasketMatch match,
            List<MatchDataSubmissionValidationItemVo> items) {
        Long submitterTeamId = submission.getSubmitterTeamId();
        if (submitterTeamId == null) {
            addItem(items, "ERROR", "提交球队缺失", false);
            return;
        }
        boolean home = Objects.equals(submitterTeamId, submission.getHomeTeamId());
        boolean away = Objects.equals(submitterTeamId, submission.getAwayTeamId());
        if (!home && !away) {
            addItem(items, "ERROR", "提交球队不属于本场比赛主客队", false);
        } else {
            addItem(items, "OK", "提交球队属于比赛双方", true);
        }
    }

    private void validatePeriods(BasketMatchDataSubmission submission, List<BasketMatchDataSubmissionPeriod> periods,
            int homeScore, int awayScore, List<MatchDataSubmissionValidationItemVo> items) {
        if (CollectionUtils.isEmpty(periods) || periods.size() < 4) {
            addItem(items, "ERROR", "分节比分至少包含4节", false);
            return;
        }
        int homeTotal = 0;
        int awayTotal = 0;
        int otCount = 0;
        for (BasketMatchDataSubmissionPeriod period : periods) {
            if (period.getPeriodNo() == null || !StringUtils.hasText(period.getPeriodType())) {
                addItem(items, "ERROR", "存在节次信息不完整", false);
                continue;
            }
            Integer hs = period.getHomeScore();
            Integer as = period.getAwayScore();
            if (hs == null || as == null) {
                addItem(items, "ERROR", "第 " + period.getPeriodNo() + " 节比分不能为空", false);
                continue;
            }
            if (hs < 0 || as < 0) {
                addItem(items, "ERROR", "第 " + period.getPeriodNo() + " 节比分不能为负数", false);
            }
            homeTotal += hs;
            awayTotal += as;
            if (MatchPeriodTypeEnum.OVERTIME.getCode().equalsIgnoreCase(period.getPeriodType())) {
                otCount++;
            }
        }
        if (homeTotal == homeScore && awayTotal == awayScore) {
            addItem(items, "OK", "分节比分合计与总分一致", true);
        } else {
            addItem(items, "ERROR",
                    "分节比分合计为 " + homeTotal + " : " + awayTotal + "，与总分 " + homeScore + " : " + awayScore + " 不一致",
                    false);
        }
        if (homeScore == awayScore && otCount == 0) {
            addItem(items, "WARN", "常规时间平局且未录入加时", false);
        }
    }

    private void validateTeamStats(BasketMatchDataSubmission submission,
            List<BasketMatchDataSubmissionTeamStat> teamStats, int homeScore, int awayScore,
            List<MatchDataSubmissionValidationItemVo> items) {
        if (CollectionUtils.isEmpty(teamStats) || teamStats.size() < 2) {
            addItem(items, "ERROR", "球队技术统计须包含主客队各一条", false);
            return;
        }
        Map<Long, BasketMatchDataSubmissionTeamStat> map = new HashMap<>();
        for (BasketMatchDataSubmissionTeamStat stat : teamStats) {
            if (stat.getTeamId() == null) {
                addItem(items, "ERROR", "球队统计缺少球队ID", false);
                continue;
            }
            if (!stat.getTeamId().equals(submission.getHomeTeamId()) && !stat.getTeamId().equals(submission.getAwayTeamId())) {
                addItem(items, "ERROR", "存在不属于本场比赛的球队统计", false);
            }
            map.put(stat.getTeamId(), stat);
        }
        BasketMatchDataSubmissionTeamStat homeStat = map.get(submission.getHomeTeamId());
        BasketMatchDataSubmissionTeamStat awayStat = map.get(submission.getAwayTeamId());
        if (homeStat == null || awayStat == null) {
            addItem(items, "ERROR", "缺少主客队球队统计", false);
            return;
        }
        if (nvl(homeStat.getPoints()) == homeScore && nvl(awayStat.getPoints()) == awayScore) {
            addItem(items, "OK", "球队得分与最终比分一致", true);
        } else {
            addItem(items, "ERROR",
                    "球队得分为 " + nvl(homeStat.getPoints()) + " : " + nvl(awayStat.getPoints())
                            + "，与总分 " + homeScore + " : " + awayScore + " 不一致",
                    false);
        }
    }

    private void validatePlayerStats(BasketMatchDataSubmission submission,
            List<BasketMatchDataSubmissionPlayerStat> playerStats,
            List<BasketMatchDataSubmissionTeamStat> teamStats,
            Long submitEventTeamId,
            List<MatchDataSubmissionValidationItemVo> items) {
        if (CollectionUtils.isEmpty(playerStats)) {
            addItem(items, "WARN", "暂无球员统计数据", false);
            return;
        }
        Set<Long> rosterIds = loadLockedRosterIds(submitEventTeamId);
        Set<String> playerKeys = new HashSet<>();
        Map<Long, int[]> teamTotals = new HashMap<>();
        teamTotals.put(submission.getHomeTeamId(), new int[3]);
        teamTotals.put(submission.getAwayTeamId(), new int[3]);

        for (BasketMatchDataSubmissionPlayerStat stat : playerStats) {
            if (stat.getTeamId() == null || stat.getPlayerId() == null) {
                addItem(items, "ERROR", "存在球员统计缺少球队或球员ID", false);
                continue;
            }
            String key = stat.getTeamId() + ":" + stat.getPlayerId();
            if (!playerKeys.add(key)) {
                addItem(items, "ERROR", "存在重复球员统计：" + stat.getPlayerId(), false);
            }
            if (nvl(stat.getPoints()) < 0 || nvl(stat.getRebounds()) < 0 || nvl(stat.getAssists()) < 0) {
                addItem(items, "ERROR", "球员统计存在负数：" + stat.getPlayerId(), false);
            }
            if (stat.getRosterId() != null && !rosterIds.isEmpty() && !rosterIds.contains(stat.getRosterId())) {
                addItem(items, "ERROR", "球员 rosterId=" + stat.getRosterId() + " 不在本届锁定阵容中", false);
            }
            int[] totals = teamTotals.get(stat.getTeamId());
            if (totals != null) {
                totals[0] += nvl(stat.getPoints());
                totals[1] += nvl(stat.getRebounds());
                totals[2] += nvl(stat.getAssists());
            }
        }

        Long submitterTeamId = submission.getSubmitterTeamId();
        int[] submitterTotals = teamTotals.getOrDefault(submitterTeamId, new int[3]);
        BasketMatchDataSubmissionTeamStat submitterTeamStat = teamStats.stream()
                .filter(item -> Objects.equals(item.getTeamId(), submitterTeamId))
                .findFirst().orElse(null);
        if (submitterTeamStat != null) {
            if (submitterTotals[0] == nvl(submitterTeamStat.getPoints())) {
                addItem(items, "OK", "提交方球员得分合计与球队得分一致", true);
            } else {
                addItem(items, "ERROR",
                        "提交方球员得分合计为 " + submitterTotals[0] + "，球队得分为 " + nvl(submitterTeamStat.getPoints())
                                + "，相差 " + Math.abs(submitterTotals[0] - nvl(submitterTeamStat.getPoints())) + " 分",
                        false);
            }
            if (submitterTotals[1] == nvl(submitterTeamStat.getRebounds())) {
                addItem(items, "OK", "提交方球员篮板合计与球队篮板一致", true);
            } else {
                addItem(items, "WARN",
                        "提交方球员篮板合计为 " + submitterTotals[1] + "，球队篮板为 " + nvl(submitterTeamStat.getRebounds()),
                        false);
            }
            if (submitterTotals[2] == nvl(submitterTeamStat.getAssists())) {
                addItem(items, "OK", "提交方球员助攻合计与球队助攻一致", true);
            } else {
                addItem(items, "WARN",
                        "提交方球员助攻合计为 " + submitterTotals[2] + "，球队助攻为 " + nvl(submitterTeamStat.getAssists()),
                        false);
            }
        }
    }

    private Set<Long> loadLockedRosterIds(Long eventTeamId) {
        if (eventTeamId == null) {
            return Set.of();
        }
        List<BasketTournamentSeasonRoster> rosters = seasonRosterMapper.selectActiveByEventTeamId(eventTeamId);
        if (CollectionUtils.isEmpty(rosters)) {
            return Set.of();
        }
        Set<Long> ids = new HashSet<>();
        for (BasketTournamentSeasonRoster roster : rosters) {
            if (roster.getId() != null) {
                ids.add(roster.getId());
            }
        }
        return ids;
    }

    private MatchDataSubmissionValidationSummaryVo summarize(List<MatchDataSubmissionValidationItemVo> items) {
        MatchDataSubmissionValidationSummaryVo summary = new MatchDataSubmissionValidationSummaryVo();
        summary.setItems(items);
        int errors = (int) items.stream().filter(item -> "ERROR".equals(item.getLevel())).count();
        int warnings = (int) items.stream().filter(item -> "WARN".equals(item.getLevel())).count();
        summary.setErrorCount(errors);
        summary.setWarningCount(warnings);
        summary.setAllPassed(errors == 0);
        return summary;
    }

    private void addItem(List<MatchDataSubmissionValidationItemVo> items, String level, String message, boolean passed) {
        MatchDataSubmissionValidationItemVo item = new MatchDataSubmissionValidationItemVo();
        item.setLevel(level);
        item.setMessage(message);
        item.setPassed(passed);
        items.add(item);
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }
}
