package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketDivisionStageTeam;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.common.exception.ServiceException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 赛程生成前后完整性校验（单循环为主）。
 */
public final class StageScheduleIntegrityValidator {

    private StageScheduleIntegrityValidator() {}

    public static void validateBeforeGenerate(StageExecutionContext context) {
        requireContext(context);
        BasketDivisionStage stage = context.getStage();
        List<BasketDivisionStageTeam> stageTeams = context.getStageTeams();
        if (stageTeams == null || stageTeams.size() < 2) {
            throw new ServiceException("阶段参赛队伍不足 2 支，无法生成赛程");
        }

        Set<Long> eventTeamIds = new HashSet<>();
        for (BasketDivisionStageTeam stageTeam : stageTeams) {
            Long eventTeamId = stageTeam.getEventTeamId();
            if (eventTeamId == null) {
                throw new ServiceException("阶段参赛队伍存在未绑定 eventTeamId 的记录，无法生成赛程");
            }
            if (!eventTeamIds.add(eventTeamId)) {
                throw new ServiceException("阶段参赛队伍存在重复 EventTeam，无法生成赛程");
            }
        }

        if (CompetitionFormatEnum.ROUND_ROBIN.getCode().equals(stage.getCompetitionFormat())
                && !AbstractStageScheduleStrategy.isGroupStage(stage)) {
            int teamCount = stageTeams.size();
            StageScheduleMathSupport.RoundRobinStats stats =
                    StageScheduleMathSupport.computeRoundRobinStats(teamCount);
            if (stats.getTotalMatches() <= 0) {
                throw new ServiceException("单循环队伍数量无效，无法生成赛程");
            }
        }
    }

    public static void validateAfterRoundRobinGenerate(StageExecutionContext context,
                                                List<BasketV4Match> matches,
                                                int expectedMatchCount) {
        requireContext(context);
        if (matches == null || matches.isEmpty()) {
            throw new ServiceException("赛程生成失败：未写入任何比赛");
        }
        if (matches.size() != expectedMatchCount) {
            throw new ServiceException(String.format(
                    "赛程生成数量不符：期望 %d 场，实际 %d 场", expectedMatchCount, matches.size()));
        }

        Set<Long> stageTeamIds = new HashSet<>();
        for (BasketDivisionStageTeam stageTeam : context.getStageTeams()) {
            if (stageTeam.getEventTeamId() != null) {
                stageTeamIds.add(stageTeam.getEventTeamId());
            }
        }

        int teamCount = stageTeamIds.size();
        boolean hasBye = teamCount % 2 == 1;
        Set<String> pairKeys = new HashSet<>();
        Set<String> matchNos = new HashSet<>();
        Map<Long, Integer> perTeamCounts = new HashMap<>();

        for (BasketV4Match match : matches) {
            Long home = match.getHomeEventTeamId();
            Long away = match.getAwayEventTeamId();
            if (home == null || away == null) {
                throw new ServiceException("赛程数据无效：存在主客队为空的比赛");
            }
            if (home.equals(away)) {
                throw new ServiceException("赛程数据无效：存在主客队相同的比赛");
            }
            if (!stageTeamIds.contains(home) || !stageTeamIds.contains(away)) {
                throw new ServiceException("赛程数据无效：存在不属于当前阶段的比赛对阵");
            }
            if (!Objects.equals(context.getStage().getId(), match.getStageId())) {
                throw new ServiceException("赛程数据无效：比赛 stageId 不匹配");
            }
            if (!Objects.equals(context.getDivision().getId(), match.getDivisionId())) {
                throw new ServiceException("赛程数据无效：比赛 divisionId 不匹配");
            }
            if (match.getRoundNo() == null || match.getRoundNo() <= 0) {
                throw new ServiceException("赛程数据无效：roundNo 无效");
            }
            if (match.getMatchNo() == null || match.getMatchNo().isEmpty()) {
                throw new ServiceException("赛程数据无效：matchNo 为空");
            }
            if (!matchNos.add(match.getMatchNo())) {
                throw new ServiceException("赛程数据无效：matchNo 重复");
            }

            long low = Math.min(home, away);
            long high = Math.max(home, away);
            if (!pairKeys.add(low + ":" + high)) {
                throw new ServiceException("赛程数据无效：存在重复对阵");
            }

            perTeamCounts.merge(home, 1, Integer::sum);
            perTeamCounts.merge(away, 1, Integer::sum);
        }

        int expectedPerTeam = teamCount - 1;
        for (Long teamId : stageTeamIds) {
            int count = perTeamCounts.getOrDefault(teamId, 0);
            if (count != expectedPerTeam) {
                throw new ServiceException(String.format(
                        "赛程数据无效：球队 %d 比赛场次应为 %d，实际 %d", teamId, expectedPerTeam, count));
            }
        }

        if (hasBye) {
            int expectedRounds = teamCount;
            Set<Integer> rounds = new HashSet<>();
            for (BasketV4Match match : matches) {
                rounds.add(match.getRoundNo());
            }
            if (rounds.size() != expectedRounds) {
                throw new ServiceException(String.format(
                        "赛程数据无效：奇数队伍轮次应为 %d，实际 %d", expectedRounds, rounds.size()));
            }
        }
    }

    private static void requireContext(StageExecutionContext context) {
        if (context == null || context.getDivision() == null || context.getStage() == null) {
            throw new ServiceException("阶段执行上下文不完整");
        }
    }
}
