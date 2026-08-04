package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketDivisionStageTeam;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.enums.v4.GenerateStatusEnum;
import com.zhs.basket.enums.v4.GroupingModeEnum;
import com.zhs.basket.enums.v4.MatchPublishStatusEnum;
import com.zhs.basket.enums.v4.MatchStatusEnum;
import com.zhs.basket.enums.v4.PublishStatusEnum;
import com.zhs.basket.enums.v4.ScheduleGenerationStrategyEnum;
import com.zhs.basket.enums.v4.StageTypeEnum;
import com.zhs.basket.mapper.v4.BasketScheduleBatchMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 阶段赛程策略公共能力。
 */
abstract class AbstractStageScheduleStrategy implements DivisionStageScheduleStrategy {

    protected static final Integer NOT_DELETED = 0;
    protected static final String GENERATE_TYPE_AUTO = "AUTO";

    @Autowired
    protected BasketScheduleBatchMapper scheduleBatchMapper;
    @Autowired
    protected BasketV4MatchMapper matchMapper;

    protected static boolean isGroupStage(BasketDivisionStage stage) {
        if (StageTypeEnum.GROUP.getCode().equals(stage.getStageType())) {
            return true;
        }
        String groupingMode = stage.getGroupingMode();
        return groupingMode != null && !GroupingModeEnum.NONE.getCode().equals(groupingMode);
    }

    protected void requireContext(StageExecutionContext context) {
        if (context == null || context.getDivision() == null || context.getStage() == null) {
            throw new ServiceException("阶段执行上下文不完整");
        }
    }

    protected void validateBestOfOne(BasketDivisionStage stage) {
        Integer bestOf = stage.getBestOf();
        if (bestOf == null || bestOf != 1) {
            throw new ServiceException("当前版本暂不支持 BO3/BO5 系列赛执行，请将 best_of 设为 1");
        }
    }

    protected void validateTeamsPresent(StageExecutionContext context) {
        List<BasketDivisionStageTeam> teams = context.getStageTeams();
        if (teams == null || teams.size() < 2) {
            throw new ServiceException("阶段参赛队伍不足 2 支，无法生成赛程");
        }
    }

    protected List<Long> resolveTeamIds(List<BasketDivisionStageTeam> stageTeams) {
        return stageTeams.stream()
                .sorted(Comparator
                        .comparing(BasketDivisionStageTeam::getSeedNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(BasketDivisionStageTeam::getEventTeamId))
                .map(BasketDivisionStageTeam::getEventTeamId)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    protected BasketScheduleBatch createBatch(StageExecutionContext context,
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
        batch.setGenerateType(GENERATE_TYPE_AUTO);
        batch.setGenerateStatus(GenerateStatusEnum.SUCCESS.getCode());
        batch.setPublishStatus(PublishStatusEnum.DRAFT.getCode());
        batch.setGenerateTime(now);
        batch.setGeneratedBy(SecurityUtils.getUserId());
        batch.setCreateTime(now);
        batch.setUpdateTime(now);
        batch.setIsDelete(NOT_DELETED);
        if (scheduleBatchMapper.insert(batch) != 1) {
            throw new ServiceException("创建赛程批次失败");
        }
        return batch;
    }

    protected BasketV4Match newMatch(StageExecutionContext context,
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

    protected void insertMatch(BasketV4Match match) {
        if (matchMapper.insert(match) != 1) {
            throw new ServiceException("写入比赛失败: " + match.getMatchNo());
        }
    }

    protected StageSchedulePreviewVo buildRoundRobinPreview(int teamCount) {
        StageScheduleMathSupport.RoundRobinStats stats = StageScheduleMathSupport.computeRoundRobinStats(teamCount);
        StageSchedulePreviewVo preview = new StageSchedulePreviewVo();
        preview.setTeamCount(teamCount);
        preview.setMatchesPerTeam(stats.getMatchesPerTeam());
        preview.setTotalMatches(stats.getTotalMatches());
        preview.setRoundCount(stats.getRoundCount());
        preview.setHasBye(stats.isHasBye());
        return preview;
    }
}
