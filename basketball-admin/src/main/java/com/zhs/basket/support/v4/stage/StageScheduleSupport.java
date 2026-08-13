package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketDivisionStageTeam;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.enums.v4.GenerateStatusEnum;
import com.zhs.basket.enums.v4.MatchPublishStatusEnum;
import com.zhs.basket.enums.v4.MatchStatusEnum;
import com.zhs.basket.enums.v4.PublishStatusEnum;
import com.zhs.basket.enums.v4.ScheduleGenerationStrategyEnum;
import com.zhs.basket.enums.v4.StructureStatusEnum;
import com.zhs.basket.support.v4.version.StructureVersionContext;
import com.zhs.basket.mapper.v4.BasketScheduleBatchMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 阶段赛程生成公共能力（供 Strategy 与 Series Generator 复用）。
 */
final class StageScheduleSupport {

    private static final Integer NOT_DELETED = 0;
    private static final String GENERATE_TYPE_AUTO = "AUTO";

    private StageScheduleSupport() {
    }

    static void requireContext(StageExecutionContext context) {
        if (context == null || context.getDivision() == null || context.getStage() == null) {
            throw new ServiceException("阶段执行上下文不完整");
        }
    }

    static BasketScheduleBatch createBatch(BasketScheduleBatchMapper scheduleBatchMapper,
                                         StageExecutionContext context,
                                         ScheduleGenerationStrategyEnum strategy,
                                         String ruleCode,
                                         int teamCount,
                                         int expectedMatchCount,
                                         Long groupingBatchId) {
        BasketDivision division = context.getDivision();
        BasketDivisionStage stage = context.getStage();
        Date now = new Date();

        BasketScheduleBatch batch = new BasketScheduleBatch();
        batch.setSeasonId(division.getSeasonId());
        batch.setDivisionId(division.getId());
        batch.setStageId(stage.getId());
        batch.setGroupingBatchId(groupingBatchId);
        batch.setBatchNo("V4-STAGE-BATCH-" + stage.getId() + "-" + System.currentTimeMillis());
        batch.setFormatType(division.getFormatType());
        batch.setRuleCode(ruleCode);
        batch.setGenerationStrategy(strategy.getCode());
        batch.setTeamCount(teamCount);
        batch.setMatchCount(expectedMatchCount);
        batch.setExpectedMatchCount(expectedMatchCount);
        batch.setActualMatchCount(expectedMatchCount);
        batch.setExecutionVersion(context.getExecutionVersion());
        StructureVersionContext versionContext = context.getVersionContext();
        if (versionContext != null) {
            batch.setRuleVersionId(versionContext.getRuleVersionId());
            batch.setRosterVersionId(versionContext.getRosterVersionId());
        }
        batch.setStructureStatus(StructureStatusEnum.ACTIVE.getCode());
        batch.setGenerateType(GENERATE_TYPE_AUTO);
        batch.setGenerateStatus(GenerateStatusEnum.SUCCESS.getCode());
        batch.setPublishStatus(PublishStatusEnum.DRAFT.getCode());
        batch.setGenerateTime(now);
        try {
            batch.setGeneratedBy(SecurityUtils.getUserId());
        } catch (Exception ex) {
            batch.setGeneratedBy(0L);
        }
        batch.setCreateTime(now);
        batch.setUpdateTime(now);
        batch.setIsDelete(NOT_DELETED);
        if (scheduleBatchMapper.insert(batch) != 1) {
            throw new ServiceException("创建赛程批次失败");
        }
        return batch;
    }

    static BasketV4Match newMatch(StageExecutionContext context,
                                  BasketScheduleBatch batch,
                                  int roundNo,
                                  Integer bracketSlot,
                                  String matchNo) {
        BasketDivision division = context.getDivision();
        BasketDivisionStage stage = context.getStage();
        Date now = new Date();

        BasketV4Match match = new BasketV4Match();
        match.setSeasonId(division.getSeasonId());
        match.setDivisionId(division.getId());
        match.setScheduleBatchId(batch.getId());
        match.setStageId(stage.getId());
        match.setStageCode(stage.getStageCode());
        match.setRoundNo(roundNo);
        match.setBracketSlot(bracketSlot);
        match.setMatchNo(matchNo);
        match.setMatchStatus(MatchStatusEnum.SCHEDULED.getCode());
        match.setPublishStatus(MatchPublishStatusEnum.DRAFT.getCode());
        match.setPlatformCode(division.getPlatformCode());
        match.setCreateTime(now);
        match.setUpdateTime(now);
        match.setIsDelete(NOT_DELETED);
        return match;
    }

    static void insertMatch(BasketV4MatchMapper matchMapper, BasketV4Match match) {
        if (matchMapper.insert(match) != 1) {
            throw new ServiceException("写入比赛失败: " + match.getMatchNo());
        }
    }

    static void validateMatchPair(BasketV4Match match,
                                StageExecutionContext context,
                                int roundNo,
                                int pairIndex,
                                Set<String> seenPairKeys) {
        Long home = match.getHomeEventTeamId();
        Long away = match.getAwayEventTeamId();
        if (home == null || away == null) {
            throw new ServiceException(String.format(
                    "赛程对阵无效：第 %d 轮第 %d 组主客队不能为空", roundNo, pairIndex));
        }
        if (home.equals(away)) {
            throw new ServiceException(String.format(
                    "赛程对阵无效：第 %d 轮第 %d 组主客队不能相同", roundNo, pairIndex));
        }
        if (match.getStageId() == null || match.getDivisionId() == null) {
            throw new ServiceException(String.format(
                    "赛程对阵无效：第 %d 轮第 %d 组缺少 stageId 或 divisionId", roundNo, pairIndex));
        }
        if (roundNo <= 0) {
            throw new ServiceException(String.format(
                    "赛程对阵无效：第 %d 轮第 %d 组 roundNo 无效", roundNo, pairIndex));
        }

        Set<Long> stageTeamIds = context.getStageTeams().stream()
                .map(BasketDivisionStageTeam::getEventTeamId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!stageTeamIds.contains(home) || !stageTeamIds.contains(away)) {
            throw new ServiceException(String.format(
                    "赛程对阵无效：第 %d 轮第 %d 组球队不属于当前阶段", roundNo, pairIndex));
        }

        Long divisionId = context.getDivision().getId();
        if (!divisionId.equals(match.getDivisionId())) {
            throw new ServiceException(String.format(
                    "赛程对阵无效：第 %d 轮第 %d 组 divisionId 不匹配", roundNo, pairIndex));
        }

        long low = Math.min(home, away);
        long high = Math.max(home, away);
        String pairKey = low + ":" + high;
        if (seenPairKeys != null && !seenPairKeys.add(pairKey)) {
            throw new ServiceException(String.format(
                    "赛程对阵无效：第 %d 轮第 %d 组出现重复对阵", roundNo, pairIndex));
        }
    }
}
