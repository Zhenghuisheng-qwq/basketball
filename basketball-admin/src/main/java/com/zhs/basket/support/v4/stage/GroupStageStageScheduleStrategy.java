package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivisionGroup;
import com.zhs.basket.domain.v4.BasketDivisionGroupingBatch;
import com.zhs.basket.domain.v4.BasketDivisionGroupTeam;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketDivisionStageTeam;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.basket.enums.v4.GroupingStatusEnum;
import com.zhs.basket.enums.v4.ScheduleGenerationStrategyEnum;
import com.zhs.basket.support.v4.StageFormatSpecification;
import com.zhs.basket.support.v4.BasketV4RoundRobinScheduler;
import com.zhs.basket.support.v4.BasketV4RoundRobinScheduler.Pairing;
import com.zhs.basket.support.v4.BasketV4RoundRobinScheduler.Round;
import com.zhs.basket.support.v4.BasketV4RoundRobinScheduler.TimeSlotAllocator;
import com.zhs.common.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 小组赛阶段赛程策略。
 */
@Component
public class GroupStageStageScheduleStrategy extends AbstractStageScheduleStrategy {

    @Override
    public boolean supports(BasketDivisionStage stage) {
        return CompetitionFormatEnum.ROUND_ROBIN.getCode().equals(stage.getCompetitionFormat())
                && StageFormatSpecification.isGrouped(stage);
    }

    @Override
    public void validate(StageExecutionContext context) {
        requireContext(context);
        validateBestOfOne(context.getStage());
        validateTeamsPresent(context);

        BasketDivisionGroupingBatch batch = context.getGroupingBatch();
        if (batch == null) {
            throw new ServiceException("小组赛阶段需先完成分组并锁定分组批次");
        }
        if (!GroupingStatusEnum.LOCKED.getCode().equals(batch.getGroupingStatus())) {
            throw new ServiceException("分组批次未锁定，无法生成小组赛赛程");
        }

        BasketDivisionStage stage = context.getStage();
        Integer expectedGroupCount = stage.getGroupCount();
        if (expectedGroupCount == null || expectedGroupCount <= 0) {
            throw new ServiceException("小组赛阶段需配置 group_count");
        }
        List<BasketDivisionGroup> groups = context.getGroups();
        if (groups == null || groups.isEmpty()) {
            throw new ServiceException("分组数据不存在，无法生成小组赛赛程");
        }
        if (groups.size() != expectedGroupCount) {
            throw new ServiceException("实际分组数与阶段配置不一致，期望 " + expectedGroupCount + " 组，实际 " + groups.size() + " 组");
        }
        if (batch.getGroupCount() != null && !batch.getGroupCount().equals(expectedGroupCount)) {
            throw new ServiceException("分组批次组数与阶段配置不一致");
        }

        for (BasketDivisionGroup group : groups) {
            if (group.getQualifyCount() == null || group.getQualifyCount() <= 0) {
                throw new ServiceException("小组「" + group.getGroupName() + "」未配置 qualify_count");
            }
        }

        Set<Long> assignedTeams = context.getGroupTeams() == null
                ? Set.of()
                : context.getGroupTeams().stream()
                .map(BasketDivisionGroupTeam::getEventTeamId)
                .collect(Collectors.toSet());
        Set<Long> stageTeamIds = context.getStageTeams().stream()
                .map(BasketDivisionStageTeam::getEventTeamId)
                .collect(Collectors.toSet());
        if (!assignedTeams.equals(stageTeamIds)) {
            throw new ServiceException("存在未分组或多余的参赛队伍，请确认所有队伍均已分组");
        }

        Map<Long, Long> teamGroupCount = new HashMap<>();
        for (BasketDivisionGroupTeam groupTeam : context.getGroupTeams()) {
            teamGroupCount.merge(groupTeam.getEventTeamId(), 1L, Long::sum);
        }
        for (Map.Entry<Long, Long> entry : teamGroupCount.entrySet()) {
            if (entry.getValue() > 1) {
                throw new ServiceException("球队不能重复出现在多个小组中");
            }
        }
    }

    @Override
    public StageSchedulePreviewVo preview(StageExecutionContext context) {
        validate(context);
        Map<Long, List<BasketDivisionGroupTeam>> teamsByGroup = groupTeamsByGroupId(context);
        List<BasketDivisionGroup> sortedGroups = sortedGroups(context.getGroups());

        StageSchedulePreviewVo preview = new StageSchedulePreviewVo();
        preview.setTeamCount(context.getStageTeams().size());
        preview.setGroupPreviews(new ArrayList<>());

        int totalMatches = 0;
        int maxRoundCount = 0;
        boolean anyBye = false;
        Integer minTeamCount = null;
        Integer maxTeamCount = null;

        for (BasketDivisionGroup group : sortedGroups) {
            List<BasketDivisionGroupTeam> groupTeams = teamsByGroup.getOrDefault(group.getId(), List.of());
            int groupTeamCount = groupTeams.size();
            if (groupTeamCount < 2) {
                throw new ServiceException("小组「" + group.getGroupName() + "」队伍不足 2 支");
            }

            StageScheduleMathSupport.RoundRobinStats stats =
                    StageScheduleMathSupport.computeRoundRobinStats(groupTeamCount);
            StageSchedulePreviewVo.GroupPreview groupPreview = new StageSchedulePreviewVo.GroupPreview();
            groupPreview.setGroupId(group.getId());
            groupPreview.setGroupCode(group.getGroupCode());
            groupPreview.setGroupName(group.getGroupName());
            groupPreview.setTeamCount(groupTeamCount);
            groupPreview.setTotalMatches(stats.getTotalMatches());
            groupPreview.setMatchesPerTeam(stats.getMatchesPerTeam());
            groupPreview.setRoundCount(stats.getRoundCount());
            groupPreview.setHasBye(stats.isHasBye());
            preview.getGroupPreviews().add(groupPreview);

            totalMatches += stats.getTotalMatches();
            maxRoundCount = Math.max(maxRoundCount, stats.getRoundCount());
            anyBye = anyBye || stats.isHasBye();
            minTeamCount = minTeamCount == null ? groupTeamCount : Math.min(minTeamCount, groupTeamCount);
            maxTeamCount = maxTeamCount == null ? groupTeamCount : Math.max(maxTeamCount, groupTeamCount);
        }

        preview.setTotalMatches(totalMatches);
        preview.setRoundCount(maxRoundCount);
        preview.setHasBye(anyBye);
        preview.setMatchesPerTeam(maxTeamCount == null ? 0 : maxTeamCount - 1);

        if (minTeamCount != null && maxTeamCount != null && !minTeamCount.equals(maxTeamCount)) {
            preview.getWarnings().add("各组队伍数不均衡（最少 " + minTeamCount + " 支，最多 " + maxTeamCount + " 支），轮空场次可能不一致");
        }
        return preview;
    }

    @Override
    public ScheduleGenerationResult generate(StageExecutionContext context) {
        validate(context);
        Map<Long, List<BasketDivisionGroupTeam>> teamsByGroup = groupTeamsByGroupId(context);
        List<BasketDivisionGroup> sortedGroups = sortedGroups(context.getGroups());

        int expectedMatchCount = 0;
        int maxRoundCount = 0;
        for (BasketDivisionGroup group : sortedGroups) {
            StageScheduleMathSupport.RoundRobinStats stats =
                    StageScheduleMathSupport.computeRoundRobinStats(teamsByGroup.get(group.getId()).size());
            expectedMatchCount += stats.getTotalMatches();
            maxRoundCount = Math.max(maxRoundCount, stats.getRoundCount());
        }

        BasketScheduleBatch batch = createBatch(context,
                ScheduleGenerationStrategyEnum.GROUP_STAGE,
                BasketV4RoundRobinScheduler.ruleCode(),
                context.getStageTeams().size(),
                expectedMatchCount,
                context.getGroupingBatch().getId());

        Date startFrom = context.getDivision().getMatchStartTime() != null
                ? context.getDivision().getMatchStartTime() : new Date();
        TimeSlotAllocator allocator = new TimeSlotAllocator(startFrom);

        Set<String> seenPairKeys = new HashSet<>();
        int globalSeq = 1;
        for (BasketDivisionGroup group : sortedGroups) {
            List<Long> teamIds = teamsByGroup.get(group.getId()).stream()
                    .sorted(Comparator
                            .comparing(BasketDivisionGroupTeam::getSeedNo, Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(BasketDivisionGroupTeam::getEventTeamId))
                    .map(BasketDivisionGroupTeam::getEventTeamId)
                    .collect(Collectors.toList());
            List<Round> rounds = BasketV4RoundRobinScheduler.generateRoundRobin(
                    BasketV4RoundRobinScheduler.sortedTeamIds(teamIds));

            for (Round round : rounds) {
                int seqInRound = 1;
                for (Pairing pairing : round.getPairings()) {
                    BasketV4Match match = newMatch(context, batch, round.getRoundNo(), null,
                            buildGroupMatchNo(context.getDivision().getId(), group.getGroupCode(), round.getRoundNo(), seqInRound));
                    match.setGroupId(group.getId());
                    match.setMatchTime(allocator.allocate());
                    match.setHomeEventTeamId(pairing.getHomeEventTeamId());
                    match.setAwayEventTeamId(pairing.getAwayEventTeamId());
                    validateAndInsertMatch(match, context, round.getRoundNo(), seqInRound, seenPairKeys);
                    seqInRound++;
                    globalSeq++;
                }
            }
        }

        return new ScheduleGenerationResult(batch.getId(), expectedMatchCount, expectedMatchCount, maxRoundCount);
    }

    private Map<Long, List<BasketDivisionGroupTeam>> groupTeamsByGroupId(StageExecutionContext context) {
        Map<Long, List<BasketDivisionGroupTeam>> result = new HashMap<>();
        if (context.getGroupTeams() == null) {
            return result;
        }
        for (BasketDivisionGroupTeam groupTeam : context.getGroupTeams()) {
            result.computeIfAbsent(groupTeam.getGroupId(), key -> new ArrayList<>()).add(groupTeam);
        }
        return result;
    }

    private List<BasketDivisionGroup> sortedGroups(List<BasketDivisionGroup> groups) {
        return groups.stream()
                .sorted(Comparator
                        .comparing(BasketDivisionGroup::getGroupOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(BasketDivisionGroup::getId))
                .collect(Collectors.toList());
    }

    private String buildGroupMatchNo(Long divisionId, String groupCode, int roundNo, int seqInRound) {
        String safeGroupCode = groupCode == null ? "G" : groupCode;
        return String.format("V4-%d-%s-%d-%02d", divisionId, safeGroupCode, roundNo, seqInRound);
    }
}
