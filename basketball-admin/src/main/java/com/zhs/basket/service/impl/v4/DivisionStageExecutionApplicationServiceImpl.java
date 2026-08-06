package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.*;
import com.zhs.basket.dto.v4.BasketStageManualRankReq;
import com.zhs.basket.enums.v4.*;
import com.zhs.basket.mapper.v4.*;
import com.zhs.basket.service.v4.IDivisionStageExecutionApplicationService;
import com.zhs.basket.support.v4.V4FormatDisplaySupport;
import com.zhs.basket.support.v4.stage.DivisionStageScheduleStrategy;
import com.zhs.basket.support.v4.stage.DivisionStageScheduleStrategyFactory;
import com.zhs.basket.support.v4.stage.ScheduleGenerationResult;
import com.zhs.basket.support.v4.stage.StageExecutionContext;
import com.zhs.basket.support.v4.stage.StageScheduleIntegrityValidator;
import com.zhs.basket.support.v4.stage.StageSchedulePrerequisiteValidator;
import com.zhs.basket.support.v4.stage.StageSchedulePreviewVo;
import com.zhs.basket.support.v4.stage.TieBreakStandingService;
import com.zhs.basket.vo.v4.*;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DivisionStageExecutionApplicationServiceImpl implements IDivisionStageExecutionApplicationService {

    private static final Integer NOT_DELETED = 0;
    private static final long OVERALL_SCOPE_KEY = 0L;

    @Autowired
    private BasketDivisionStageMapper stageMapper;
    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private BasketDivisionStageTeamMapper stageTeamMapper;
    @Autowired
    private BasketDivisionStageQualificationMapper qualificationMapper;
    @Autowired
    private BasketEventTeamMapper eventTeamMapper;
    @Autowired
    private BasketScheduleBatchMapper scheduleBatchMapper;
    @Autowired
    private BasketV4MatchMapper matchMapper;
    @Autowired
    private BasketMatchSubmissionMapper submissionMapper;
    @Autowired
    private BasketStandingMapper standingMapper;
    @Autowired
    private BasketStandingRankReasonMapper rankReasonMapper;
    @Autowired
    private BasketDivisionGroupingBatchMapper groupingBatchMapper;
    @Autowired
    private BasketDivisionGroupMapper groupMapper;
    @Autowired
    private BasketDivisionGroupTeamMapper groupTeamMapper;
    @Autowired
    private DivisionStageScheduleStrategyFactory scheduleStrategyFactory;
    @Autowired
    private TieBreakStandingService tieBreakStandingService;
    @Autowired
    private StageSchedulePrerequisiteValidator schedulePrerequisiteValidator;

    @Override
    public List<BasketDivisionStageExecutionListVo> listStages(Long divisionId) {
        requireDivision(divisionId);
        List<BasketDivisionStage> stages = stageMapper.selectByDivisionId(divisionId);
        List<BasketDivisionStageExecutionListVo> result = new ArrayList<>();
        for (BasketDivisionStage stage : stages) {
            result.add(toExecutionListVo(stage));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BasketStageTeamSyncResultVo initializeStageTeams(Long stageId) {
        BasketDivisionStage stage = requireStage(stageId);
        BasketDivision division = requireDivision(stage.getDivisionId());
        Date now = new Date();

        List<BasketDivisionStageTeam> existingStageTeams = stageTeamMapper.selectByStageId(stageId);
        Set<Long> existingEventTeamIds = existingStageTeams.stream()
                .map(BasketDivisionStageTeam::getEventTeamId)
                .collect(Collectors.toSet());

        List<BasketDivisionStageTeam> toInsert = new ArrayList<>();
        int totalSourceCount;

        if (isFirstStage(stage)) {
            List<BasketEventTeam> eventTeams = eventTeamMapper.selectActiveByDivisionId(division.getId());
            totalSourceCount = eventTeams.size();
            int nextSeed = existingStageTeams.stream()
                    .map(BasketDivisionStageTeam::getSeedNo)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(0) + 1;
            for (BasketEventTeam team : eventTeams) {
                if (existingEventTeamIds.contains(team.getId())) {
                    continue;
                }
                toInsert.add(buildStageTeam(stage, team.getId(), null, null, null,
                        nextSeed++, StageTeamEntryTypeEnum.INITIAL.getCode(), now));
            }
        } else {
            BasketDivisionStage previousStage = findPreviousStage(stage);
            if (previousStage == null) {
                throw new ServiceException("未找到上一阶段，无法初始化参赛队伍");
            }
            List<BasketDivisionStageQualification> confirmed =
                    qualificationMapper.selectConfirmedByStageId(previousStage.getId());
            if (confirmed.isEmpty() && existingStageTeams.isEmpty()) {
                throw new ServiceException("上一阶段尚无已确认晋级结果");
            }
            totalSourceCount = confirmed.size();
            int nextSeed = existingStageTeams.stream()
                    .map(BasketDivisionStageTeam::getSeedNo)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(0) + 1;
            for (BasketDivisionStageQualification qualification : confirmed) {
                if (existingEventTeamIds.contains(qualification.getEventTeamId())) {
                    continue;
                }
                StageTeamEntryTypeEnum entryType = mapEntryType(qualification.getQualificationType());
                toInsert.add(buildStageTeam(stage, qualification.getEventTeamId(),
                        previousStage.getId(), qualification.getGroupId(), qualification.getSourceRank(),
                        nextSeed++, entryType.getCode(), now));
            }
        }

        if (!toInsert.isEmpty()) {
            assertCanSyncNewStageTeams(stage);
            if (stageTeamMapper.batchInsert(toInsert) != toInsert.size()) {
                throw new ServiceException("初始化阶段参赛队伍失败");
            }
        } else if (existingStageTeams.isEmpty()) {
            throw new ServiceException("无可初始化的参赛队伍");
        }

        int finalCount = existingStageTeams.size() + toInsert.size();
        stage.setParticipantCount(finalCount);
        if (StageExecutionStatusEnum.DRAFT.getCode().equals(stage.getExecutionStatus())
                || stage.getExecutionStatus() == null) {
            stage.setExecutionStatus(StageExecutionStatusEnum.READY.getCode());
        }
        stage.setUpdateTime(now);
        stageMapper.updateExecutionStatus(stage);

        BasketStageTeamSyncResultVo result = new BasketStageTeamSyncResultVo();
        result.setTotalEventTeam(totalSourceCount);
        result.setExistingStageTeam(existingStageTeams.size());
        result.setInsertedStageTeam(toInsert.size());
        result.setSkippedStageTeam(Math.max(0, totalSourceCount - existingStageTeams.size() - toInsert.size()));
        return result;
    }

    @Override
    public StageSchedulePreviewVo previewSchedule(Long stageId) {
        BasketDivisionStage stage = requireStage(stageId);
        StageExecutionContext context = buildExecutionContext(stage);
        DivisionStageScheduleStrategy strategy = scheduleStrategyFactory.resolve(stage);
        return strategy.preview(context);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateSchedule(Long stageId) {
        BasketDivisionStage stage = requireStage(stageId);
        BasketDivision division = requireDivision(stage.getDivisionId());
        validateStageForGenerate(stage);
        schedulePrerequisiteValidator.validateBeforeGenerateSchedule(division, stage, isFirstStage(stage));

        initializeStageTeams(stageId);
        stage = requireStage(stageId);

        BasketScheduleBatch existingDraft = scheduleBatchMapper.selectDraftByStageId(stageId);
        if (existingDraft != null) {
            throw new ServiceException("该阶段已有草稿赛程，请先作废后再生成");
        }

        StageExecutionContext context = buildExecutionContext(stage);
        StageScheduleIntegrityValidator.validateBeforeGenerate(context);
        DivisionStageScheduleStrategy strategy = scheduleStrategyFactory.resolve(stage);
        strategy.validate(context);

        ScheduleGenerationResult result = strategy.generate(context);
        if (CompetitionFormatEnum.ROUND_ROBIN.getCode().equals(stage.getCompetitionFormat())
                && !isGroupStage(stage)) {
            List<BasketV4Match> generatedMatches = matchMapper.selectByStageId(stageId).stream()
                    .filter(match -> result.getBatchId().equals(match.getScheduleBatchId()))
                    .collect(Collectors.toList());
            StageScheduleIntegrityValidator.validateAfterRoundRobinGenerate(
                    context, generatedMatches, result.getExpectedMatchCount());
        }
        Date now = new Date();
        int executionVersion = stage.getExecutionVersion() != null ? stage.getExecutionVersion() + 1 : 1;

        stage.setExecutionStatus(StageExecutionStatusEnum.SCHEDULE_GENERATED.getCode());
        stage.setScheduleBatchId(result.getBatchId());
        stage.setExecutionVersion(executionVersion);
        stage.setUpdateTime(now);
        if (stageMapper.updateExecutionStatus(stage) != 1) {
            throw new ServiceException("更新阶段执行状态失败");
        }
        return result.getBatchId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishSchedule(Long stageId) {
        BasketDivisionStage stage = requireStage(stageId);
        BasketDivision division = requireDivision(stage.getDivisionId());
        schedulePrerequisiteValidator.validateBeforePublishSchedule(division, stage);
        if (stage.getScheduleBatchId() == null) {
            throw new ServiceException("阶段尚未生成赛程");
        }
        BasketScheduleBatch batch = requireBatch(stage.getScheduleBatchId());
        if (!PublishStatusEnum.DRAFT.getCode().equals(batch.getPublishStatus())) {
            throw new ServiceException("仅草稿赛程可发布");
        }
        if (matchMapper.countByBatchId(batch.getId()) <= 0) {
            throw new ServiceException("赛程批次下无比赛，无法发布");
        }

        Date now = new Date();
        Long operatorId = SecurityUtils.getUserId();
        batch.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
        batch.setPublishTime(now);
        batch.setPublishedBy(operatorId);
        batch.setUpdateTime(now);
        if (scheduleBatchMapper.updateById(batch) != 1) {
            throw new ServiceException("发布赛程批次失败");
        }
        matchMapper.publishByBatchId(batch.getId());

        stage.setExecutionStatus(StageExecutionStatusEnum.RUNNING.getCode());
        if (stage.getStartedTime() == null) {
            stage.setStartedTime(now);
        }
        stage.setUpdateTime(now);
        stageMapper.updateExecutionStatus(stage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelDraftSchedule(Long stageId) {
        BasketDivisionStage stage = requireStage(stageId);
        if (stage.getScheduleBatchId() == null) {
            throw new ServiceException("阶段无关联赛程批次");
        }
        BasketScheduleBatch batch = requireBatch(stage.getScheduleBatchId());
        if (!PublishStatusEnum.DRAFT.getCode().equals(batch.getPublishStatus())) {
            throw new ServiceException("仅草稿赛程可作废");
        }
        if (submissionMapper.countByScheduleBatchId(batch.getId()) > 0) {
            throw new ServiceException("该赛程已有比赛提交记录，无法作废");
        }

        matchMapper.softDeleteDraftByBatchId(batch.getId());
        if (scheduleBatchMapper.softDelete(batch.getId()) != 1) {
            throw new ServiceException("作废赛程批次失败");
        }

        stage.setScheduleBatchId(null);
        stage.setExecutionStatus(StageExecutionStatusEnum.READY.getCode());
        stage.setUpdateTime(new Date());
        stageMapper.updateExecutionStatus(stage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkStageCompletion(Long stageId) {
        BasketDivisionStage stage = requireStage(stageId);
        if (!StageExecutionStatusEnum.RUNNING.getCode().equals(stage.getExecutionStatus())) {
            return;
        }
        int pending = matchMapper.countPendingByStageId(stageId);
        if (pending > 0) {
            return;
        }
        int finished = matchMapper.countFinishedByStageId(stageId);
        if (finished <= 0) {
            return;
        }

        if (supportsStanding(stage)) {
            tieBreakStandingService.rebuildStageStanding(stageId);
        }

        Date now = new Date();
        stage.setExecutionStatus(StageExecutionStatusEnum.RESULT_PENDING_CONFIRM.getCode());
        stage.setCompletedTime(now);
        stage.setUpdateTime(now);
        stageMapper.updateExecutionStatus(stage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BasketStageQualificationPreviewVo previewQualification(Long stageId) {
        BasketDivisionStage stage = requireStage(stageId);
        if (!StageExecutionStatusEnum.RESULT_PENDING_CONFIRM.getCode().equals(stage.getExecutionStatus())
                && !StageExecutionStatusEnum.RUNNING.getCode().equals(stage.getExecutionStatus())) {
            throw new ServiceException("阶段尚未进入可预览晋级状态");
        }

        if (supportsStanding(stage)) {
            tieBreakStandingService.rebuildStageStanding(stageId);
        }

        qualificationMapper.softDeletePreviewByStageId(stageId);
        BasketDivisionStage nextStage = findNextStage(stage);
        List<BasketDivisionStageQualification> previewRows = buildPreviewQualifications(stage, nextStage);
        if (!previewRows.isEmpty() && qualificationMapper.batchInsert(previewRows) != previewRows.size()) {
            throw new ServiceException("保存晋级预览失败");
        }

        return buildQualificationPreviewVo(stage, nextStage, previewRows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmQualification(Long stageId) {
        BasketDivisionStage stage = requireStage(stageId);
        if (StageExecutionStatusEnum.COMPLETED.getCode().equals(stage.getExecutionStatus())) {
            return;
        }
        if (!StageExecutionStatusEnum.RESULT_PENDING_CONFIRM.getCode().equals(stage.getExecutionStatus())) {
            throw new ServiceException("阶段尚未进入待确认晋级状态");
        }
        if (tieBreakStandingService.hasManualRankPending(stageId)) {
            throw new ServiceException("存在待人工裁定的同分排名，请先处理积分榜后再确认晋级");
        }

        List<BasketDivisionStageQualification> previewRows = qualificationMapper.selectPreviewByStageId(stageId);
        if (previewRows.isEmpty()) {
            throw new ServiceException("请先预览晋级结果");
        }

        Date now = new Date();
        Long operatorId = SecurityUtils.getUserId();
        for (BasketDivisionStageQualification row : previewRows) {
            row.setQualificationStatus(QualificationRecordStatusEnum.CONFIRMED.getCode());
            row.setConfirmedBy(operatorId);
            row.setConfirmedTime(now);
            row.setActiveUniqueKey(buildQualificationActiveKey(row));
            qualificationMapper.updateStatus(row);
        }

        stage.setExecutionStatus(StageExecutionStatusEnum.COMPLETED.getCode());
        stage.setQualifiedCount(previewRows.size());
        stage.setConfirmedBy(operatorId);
        stage.setConfirmedTime(now);
        stage.setUpdateTime(now);
        stageMapper.updateExecutionStatus(stage);

        BasketDivisionStage nextStage = findNextStage(stage);
        if (nextStage != null) {
            initializeStageTeams(nextStage.getId());
            nextStage = requireStage(nextStage.getId());
            if (StageExecutionStatusEnum.DRAFT.getCode().equals(nextStage.getExecutionStatus())
                    || nextStage.getExecutionStatus() == null) {
                nextStage.setExecutionStatus(StageExecutionStatusEnum.READY.getCode());
                nextStage.setUpdateTime(now);
                stageMapper.updateExecutionStatus(nextStage);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelQualificationConfirm(Long stageId) {
        BasketDivisionStage stage = requireStage(stageId);
        if (!StageExecutionStatusEnum.COMPLETED.getCode().equals(stage.getExecutionStatus())) {
            throw new ServiceException("阶段尚未确认晋级");
        }

        BasketDivisionStage nextStage = findNextStage(stage);
        if (nextStage != null) {
            if (nextStage.getScheduleBatchId() != null) {
                throw new ServiceException("下一阶段已生成赛程，无法撤销晋级确认");
            }
            if (scheduleBatchMapper.selectDraftByStageId(nextStage.getId()) != null
                    || scheduleBatchMapper.selectPublishedByStageId(nextStage.getId()) != null) {
                throw new ServiceException("下一阶段已生成赛程，无法撤销晋级确认");
            }
            stageTeamMapper.softDeleteByStageId(nextStage.getId());
            nextStage.setParticipantCount(null);
            nextStage.setExecutionStatus(StageExecutionStatusEnum.DRAFT.getCode());
            nextStage.setUpdateTime(new Date());
            stageMapper.updateExecutionStatus(nextStage);
        }

        List<BasketDivisionStageQualification> confirmed = qualificationMapper.selectConfirmedByStageId(stageId);
        if (confirmed.isEmpty()) {
            throw new ServiceException("无可撤销的晋级确认记录");
        }
        qualificationMapper.revertConfirmedToPreviewByStageId(stageId);

        stage.setExecutionStatus(StageExecutionStatusEnum.RESULT_PENDING_CONFIRM.getCode());
        stage.setQualifiedCount(null);
        stage.setConfirmedBy(null);
        stage.setConfirmedTime(null);
        stage.setUpdateTime(new Date());
        stageMapper.updateExecutionStatus(stage);
    }

    @Override
    public List<BasketStageStandingScopeVo> listStandings(Long stageId) {
        BasketDivisionStage stage = requireStage(stageId);
        List<BasketStanding> rows = standingMapper.selectByStageId(stageId);
        if (rows.isEmpty()) {
            return List.of();
        }

        Map<Long, String> teamNames = eventTeamMapper.selectActiveByDivisionId(stage.getDivisionId()).stream()
                .collect(Collectors.toMap(BasketEventTeam::getId,
                        t -> t.getTeamNameSnapshot() != null ? t.getTeamNameSnapshot() : String.valueOf(t.getId()),
                        (a, b) -> a));
        List<Long> standingIds = rows.stream().map(BasketStanding::getId).collect(Collectors.toList());
        Map<Long, List<BasketStandingRankReason>> reasonMap = rankReasonMapper.selectByStandingIds(standingIds).stream()
                .collect(Collectors.groupingBy(BasketStandingRankReason::getStandingId));

        Map<Long, BasketStageStandingScopeVo> scopes = new LinkedHashMap<>();
        for (BasketStanding standing : rows) {
            Long scopeKey = standing.getGroupScopeKey() != null ? standing.getGroupScopeKey() : OVERALL_SCOPE_KEY;
            BasketStageStandingScopeVo scopeVo = scopes.computeIfAbsent(scopeKey, key -> {
                BasketStageStandingScopeVo vo = new BasketStageStandingScopeVo();
                vo.setGroupScopeKey(scopeKey);
                vo.setGroupId(scopeKey == OVERALL_SCOPE_KEY ? null : scopeKey);
                if (scopeKey != OVERALL_SCOPE_KEY) {
                    BasketDivisionGroup group = groupMapper.selectById(scopeKey);
                    if (group != null) {
                        vo.setGroupCode(group.getGroupCode());
                        vo.setGroupName(group.getGroupName());
                    }
                }
                return vo;
            });
            if (Integer.valueOf(1).equals(standing.getManualRankPending())) {
                scopeVo.setManualRankPending(true);
            }
            BasketStageStandingRowVo rowVo = new BasketStageStandingRowVo();
            rowVo.setEventTeamId(standing.getEventTeamId());
            rowVo.setTeamName(teamNames.get(standing.getEventTeamId()));
            rowVo.setRankNo(standing.getRankNo());
            rowVo.setWinCount(standing.getWinCount());
            rowVo.setLoseCount(standing.getLoseCount());
            rowVo.setPointFor(standing.getPointFor());
            rowVo.setPointAgainst(standing.getPointAgainst());
            rowVo.setPointDiff(standing.getPointDiff());
            rowVo.setStandingPoints(standing.getStandingPoints());
            rowVo.setManualRankPending(standing.getManualRankPending());
            rowVo.setManualRankNo(standing.getManualRankNo());
            rowVo.setManualRankReason(standing.getManualRankReason());
            List<BasketStandingRankReasonVo> reasonVos = reasonMap.getOrDefault(standing.getId(), List.of()).stream()
                    .map(this::toRankReasonVo)
                    .collect(Collectors.toList());
            rowVo.setRankReasons(reasonVos);
            reasonVos.stream()
                    .filter(r -> Integer.valueOf(1).equals(r.getIsDecisive()))
                    .findFirst()
                    .ifPresent(r -> rowVo.setDecisiveReasonText(r.getReasonText()));
            scopeVo.getRows().add(rowVo);
        }
        return new ArrayList<>(scopes.values());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyManualRank(BasketStageManualRankReq req) {
        if (req == null || req.getStageId() == null) {
            throw new ServiceException("stageId 不能为空");
        }
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new ServiceException("请提供人工排名明细");
        }
        if (req.getReason() == null || req.getReason().trim().isEmpty()) {
            throw new ServiceException("请填写人工排名原因");
        }
        BasketDivisionStage stage = requireStage(req.getStageId());
        if (StageExecutionStatusEnum.COMPLETED.getCode().equals(stage.getExecutionStatus())) {
            throw new ServiceException("该阶段已确认晋级，不可再修改人工排名");
        }
        if (!isStandingStage(stage)) {
            throw new ServiceException("仅循环赛/小组赛阶段支持人工排名");
        }
        Long groupScopeKey = req.getGroupScopeKey() != null ? req.getGroupScopeKey() : OVERALL_SCOPE_KEY;

        List<BasketStanding> scopeRows = standingMapper.selectByStageScope(
                stage.getDivisionId(), stage.getId(), groupScopeKey);
        List<BasketStanding> pendingRows = scopeRows.stream()
                .filter(row -> Integer.valueOf(1).equals(row.getManualRankPending()))
                .collect(Collectors.toList());
        if (pendingRows.isEmpty()) {
            throw new ServiceException("当前分组无需人工排名");
        }
        Set<Long> pendingTeamIds = pendingRows.stream()
                .map(BasketStanding::getEventTeamId).collect(Collectors.toSet());
        Set<Long> requestTeamIds = req.getItems().stream()
                .map(BasketStageManualRankReq.ManualRankItem::getEventTeamId)
                .collect(Collectors.toSet());
        if (!pendingTeamIds.equals(requestTeamIds)) {
            throw new ServiceException("人工排名必须覆盖全部同分待处理球队，且不能包含其他球队");
        }
        Set<Integer> rankNos = new HashSet<>();
        for (BasketStageManualRankReq.ManualRankItem item : req.getItems()) {
            if (item.getManualRankNo() == null || item.getManualRankNo() <= 0) {
                throw new ServiceException("人工排名序号无效");
            }
            if (!rankNos.add(item.getManualRankNo())) {
                throw new ServiceException("人工排名序号不能重复");
            }
        }

        Date now = new Date();
        Long operatorId = SecurityUtils.getUserId();
        Map<Long, Integer> manualRankMap = req.getItems().stream()
                .collect(Collectors.toMap(BasketStageManualRankReq.ManualRankItem::getEventTeamId,
                        BasketStageManualRankReq.ManualRankItem::getManualRankNo));
        for (BasketStanding row : pendingRows) {
            row.setManualRankNo(manualRankMap.get(row.getEventTeamId()));
            row.setManualRankReason(req.getReason().trim());
            row.setManualRankBy(operatorId);
            row.setManualRankTime(now);
            row.setManualRankPending(0);
            row.setUpdateTime(now);
            standingMapper.updateById(row);
        }

        List<BasketStanding> resorted = new ArrayList<>(scopeRows);
        resorted.sort(Comparator
                .comparing(BasketStanding::getStandingPoints, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(row -> row.getManualRankNo() != null ? row.getManualRankNo() : Integer.MAX_VALUE)
                .thenComparing(BasketStanding::getWinCount, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(BasketStanding::getPointDiff, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(BasketStanding::getEventTeamId));
        int rank = 1;
        for (BasketStanding row : resorted) {
            row.setRankNo(rank++);
            row.setUpdateTime(now);
            standingMapper.updateById(row);
        }

        qualificationMapper.softDeletePreviewByStageId(stage.getId());
    }

    private BasketStandingRankReasonVo toRankReasonVo(BasketStandingRankReason reason) {
        BasketStandingRankReasonVo vo = new BasketStandingRankReasonVo();
        vo.setRuleCode(reason.getRuleCode());
        vo.setPriorityNo(reason.getPriorityNo());
        vo.setCompareValue(reason.getCompareValue());
        vo.setReasonText(reason.getReasonText());
        vo.setIsDecisive(reason.getIsDecisive());
        try {
            vo.setRuleName(TieBreakRuleEnum.fromCode(reason.getRuleCode()).getDescription());
        } catch (Exception ignored) {
            vo.setRuleName(reason.getRuleCode());
        }
        return vo;
    }

    private List<BasketDivisionStageQualification> buildPreviewQualifications(BasketDivisionStage stage,
                                                                                BasketDivisionStage nextStage) {
        Date now = new Date();
        Long targetStageId = nextStage != null ? nextStage.getId() : null;
        CompetitionFormatEnum format = CompetitionFormatEnum.fromCode(stage.getCompetitionFormat());
        if (format == null) {
            throw new ServiceException("阶段赛制配置无效");
        }

        return switch (format) {
            case ROUND_ROBIN -> buildRoundRobinPreview(stage, nextStage, targetStageId, now);
            case SINGLE_ELIMINATION -> buildEliminationPreview(stage, nextStage, targetStageId, now);
            case MANUAL -> List.of();
        };
    }

    private List<BasketDivisionStageQualification> buildRoundRobinPreview(BasketDivisionStage stage,
                                                                          BasketDivisionStage nextStage,
                                                                          Long targetStageId,
                                                                          Date now) {
        if (isGroupStage(stage)) {
            return buildGroupStagePreview(stage, nextStage, targetStageId, now);
        }
        int topN = requireAdvancementCount(stage);
        List<BasketStanding> standings = standingMapper.selectByStageScope(
                stage.getDivisionId(), stage.getId(), OVERALL_SCOPE_KEY);
        return toPreviewRows(stage, targetStageId, now,
                standings.stream().limit(topN).collect(Collectors.toList()),
                QualificationTypeEnum.OVERALL_TOP_N, null);
    }

    private List<BasketDivisionStageQualification> buildGroupStagePreview(BasketDivisionStage stage,
                                                                          BasketDivisionStage nextStage,
                                                                          Long targetStageId,
                                                                          Date now) {
        int topN = requireAdvancementCount(stage);
        List<BasketDivisionStageQualification> result = new ArrayList<>();
        Map<Long, List<BasketStanding>> grouped = standingMapper.selectByStageId(stage.getId()).stream()
                .filter(row -> row.getGroupScopeKey() != null && row.getGroupScopeKey() != OVERALL_SCOPE_KEY)
                .collect(Collectors.groupingBy(BasketStanding::getGroupScopeKey));

        for (Map.Entry<Long, List<BasketStanding>> entry : grouped.entrySet()) {
            List<BasketStanding> topRows = entry.getValue().stream()
                    .sorted(Comparator.comparing(BasketStanding::getRankNo, Comparator.nullsLast(Integer::compareTo)))
                    .limit(topN)
                    .collect(Collectors.toList());
            result.addAll(toPreviewRows(stage, targetStageId, now, topRows,
                    QualificationTypeEnum.GROUP_TOP_N, entry.getKey()));
        }
        return result;
    }

    private List<BasketDivisionStageQualification> buildEliminationPreview(BasketDivisionStage stage,
                                                                           BasketDivisionStage nextStage,
                                                                           Long targetStageId,
                                                                           Date now) {
        List<BasketV4Match> finished = matchMapper.selectFinishedByStageId(stage.getId());
        if (finished.isEmpty()) {
            throw new ServiceException("阶段尚无已结束比赛，无法预览晋级");
        }
        int maxRound = finished.stream()
                .map(BasketV4Match::getRoundNo)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
        List<BasketV4Match> finalRound = finished.stream()
                .filter(match -> Objects.equals(match.getRoundNo(), maxRound))
                .filter(match -> match.getWinnerEventTeamId() != null)
                .collect(Collectors.toList());

        List<BasketDivisionStageQualification> result = new ArrayList<>();
        int rank = 1;
        for (BasketV4Match match : finalRound) {
            QualificationTypeEnum type = nextStage == null
                    ? QualificationTypeEnum.CHAMPION : QualificationTypeEnum.MATCH_WINNER;
            BasketDivisionStageQualification row = new BasketDivisionStageQualification();
            row.setDivisionId(stage.getDivisionId());
            row.setStageId(stage.getId());
            row.setEventTeamId(match.getWinnerEventTeamId());
            row.setQualificationType(type.getCode());
            row.setSourceRank(rank++);
            row.setTargetStageId(targetStageId);
            row.setQualificationStatus(QualificationRecordStatusEnum.PREVIEW.getCode());
            row.setActiveUniqueKey(buildQualificationActiveKey(row));
            row.setCreateTime(now);
            row.setUpdateTime(now);
            row.setIsDelete(NOT_DELETED);
            result.add(row);
        }
        return result;
    }

    private List<BasketDivisionStageQualification> toPreviewRows(BasketDivisionStage stage,
                                                                 Long targetStageId,
                                                                 Date now,
                                                                 List<BasketStanding> standings,
                                                                 QualificationTypeEnum type,
                                                                 Long groupId) {
        List<BasketDivisionStageQualification> result = new ArrayList<>();
        for (BasketStanding standing : standings) {
            BasketDivisionStageQualification row = new BasketDivisionStageQualification();
            row.setDivisionId(stage.getDivisionId());
            row.setStageId(stage.getId());
            row.setGroupId(groupId);
            row.setEventTeamId(standing.getEventTeamId());
            row.setQualificationType(type.getCode());
            row.setSourceRank(standing.getRankNo());
            row.setTargetStageId(targetStageId);
            row.setQualificationStatus(QualificationRecordStatusEnum.PREVIEW.getCode());
            row.setActiveUniqueKey(buildQualificationActiveKey(row));
            row.setCreateTime(now);
            row.setUpdateTime(now);
            row.setIsDelete(NOT_DELETED);
            result.add(row);
        }
        return result;
    }

    private BasketStageQualificationPreviewVo buildQualificationPreviewVo(BasketDivisionStage stage,
                                                                          BasketDivisionStage nextStage,
                                                                          List<BasketDivisionStageQualification> rows) {
        BasketStageQualificationPreviewVo vo = new BasketStageQualificationPreviewVo();
        vo.setStageId(stage.getId());
        vo.setStageCode(stage.getStageCode());
        vo.setStageName(stage.getStageName());
        if (nextStage != null) {
            vo.setTargetStageId(nextStage.getId());
            vo.setTargetStageCode(nextStage.getStageCode());
            vo.setTargetStageName(nextStage.getStageName());
        }
        vo.setManualRankPending(tieBreakStandingService.hasManualRankPending(stage.getId()));
        for (BasketDivisionStageQualification row : rows) {
            BasketStageQualificationItemVo item = new BasketStageQualificationItemVo();
            item.setQualificationId(row.getId());
            item.setEventTeamId(row.getEventTeamId());
            item.setGroupId(row.getGroupId());
            item.setQualificationType(row.getQualificationType());
            try {
                item.setQualificationTypeName(
                        QualificationTypeEnum.fromCode(row.getQualificationType()).getDescription());
            } catch (Exception ignored) {
                item.setQualificationTypeName(row.getQualificationType());
            }
            item.setSourceRank(row.getSourceRank());
            item.setQualificationStatus(row.getQualificationStatus());
            vo.getItems().add(item);
        }
        return vo;
    }

    private StageExecutionContext buildExecutionContext(BasketDivisionStage stage) {
        BasketDivision division = requireDivision(stage.getDivisionId());
        StageExecutionContext context = new StageExecutionContext();
        context.setDivision(division);
        context.setStage(stage);
        context.setStageTeams(stageTeamMapper.selectByStageId(stage.getId()));
        context.setExecutionVersion(stage.getExecutionVersion() != null ? stage.getExecutionVersion() : 0);

        if (isGroupStage(stage)) {
            BasketDivisionGroupingBatch groupingBatch = groupingBatchMapper.selectActiveByDivisionAndStage(
                    division.getId(), stage.getId());
            if (groupingBatch != null
                    && GroupingStatusEnum.LOCKED.getCode().equals(groupingBatch.getGroupingStatus())) {
                context.setGroupingBatch(groupingBatch);
                context.setGroups(groupMapper.selectByBatchId(groupingBatch.getId()));
                context.setGroupTeams(groupTeamMapper.selectByBatchId(groupingBatch.getId()));
            }
        }
        return context;
    }

    private BasketDivisionStageTeam buildStageTeam(BasketDivisionStage stage,
                                                   Long eventTeamId,
                                                   Long sourceStageId,
                                                   Long sourceGroupId,
                                                   Integer sourceRank,
                                                   int seedNo,
                                                   String entryType,
                                                   Date now) {
        BasketDivisionStageTeam team = new BasketDivisionStageTeam();
        team.setDivisionId(stage.getDivisionId());
        team.setSnapshotId(stage.getSnapshotId());
        team.setStageId(stage.getId());
        team.setEventTeamId(eventTeamId);
        team.setSourceStageId(sourceStageId);
        team.setSourceGroupId(sourceGroupId);
        team.setSourceRank(sourceRank);
        team.setSeedNo(seedNo);
        team.setEntryType(entryType);
        team.setQualificationStatus(StageTeamQualificationStatusEnum.PENDING.getCode());
        team.setActiveUniqueKey(stage.getId() + "_" + eventTeamId);
        team.setCreateTime(now);
        team.setUpdateTime(now);
        team.setIsDelete(NOT_DELETED);
        return team;
    }

    private void assertCanSyncNewStageTeams(BasketDivisionStage stage) {
        if (scheduleBatchMapper.selectPublishedByStageId(stage.getId()) != null) {
            throw schedulePrerequisiteValidator.businessError(StageSchedulePrerequisiteValidator.MSG_PUBLISHED_NO_MORE_TEAMS);
        }
        if (scheduleBatchMapper.selectDraftByStageId(stage.getId()) != null) {
            throw new ServiceException("该阶段已有草稿赛程，请先作废草稿赛程后再同步参赛队伍");
        }
        String status = stage.getExecutionStatus();
        if (StageExecutionStatusEnum.SCHEDULE_GENERATED.getCode().equals(status)
                || StageExecutionStatusEnum.RUNNING.getCode().equals(status)
                || StageExecutionStatusEnum.RESULT_PENDING_CONFIRM.getCode().equals(status)
                || StageExecutionStatusEnum.COMPLETED.getCode().equals(status)) {
            throw schedulePrerequisiteValidator.businessError(StageSchedulePrerequisiteValidator.MSG_PUBLISHED_NO_MORE_TEAMS);
        }
    }

    private void validateStageForGenerate(BasketDivisionStage stage) {
        String status = stage.getExecutionStatus();
        if (StageExecutionStatusEnum.SCHEDULE_GENERATED.getCode().equals(status)
                || StageExecutionStatusEnum.RUNNING.getCode().equals(status)
                || StageExecutionStatusEnum.RESULT_PENDING_CONFIRM.getCode().equals(status)
                || StageExecutionStatusEnum.COMPLETED.getCode().equals(status)) {
            throw new ServiceException("当前阶段状态不允许生成赛程");
        }
        if (scheduleBatchMapper.selectPublishedByStageId(stage.getId()) != null) {
            throw new ServiceException("该阶段已有已发布赛程");
        }
    }

    private boolean supportsStanding(BasketDivisionStage stage) {
        CompetitionFormatEnum format = CompetitionFormatEnum.fromCode(stage.getCompetitionFormat());
        return format == CompetitionFormatEnum.ROUND_ROBIN;
    }

    private boolean isGroupStage(BasketDivisionStage stage) {
        if (StageTypeEnum.GROUP.getCode().equals(stage.getStageType())) {
            return true;
        }
        String groupingMode = stage.getGroupingMode();
        return groupingMode != null && !GroupingModeEnum.NONE.getCode().equals(groupingMode);
    }

    private boolean isFirstStage(BasketDivisionStage stage) {
        BasketDivisionStage first = stageMapper.selectFirstByDivisionId(stage.getDivisionId());
        return first != null && first.getId().equals(stage.getId());
    }

    private BasketDivisionStage findPreviousStage(BasketDivisionStage stage) {
        List<BasketDivisionStage> stages = stageMapper.selectByDivisionId(stage.getDivisionId());
        BasketDivisionStage previous = null;
        for (BasketDivisionStage item : stages) {
            if (item.getStageOrder() != null && stage.getStageOrder() != null
                    && item.getStageOrder() < stage.getStageOrder()) {
                if (previous == null || item.getStageOrder() > previous.getStageOrder()) {
                    previous = item;
                }
            }
        }
        return previous;
    }

    private BasketDivisionStage findNextStage(BasketDivisionStage stage) {
        if (stage.getNextStageCode() == null || stage.getNextStageCode().isEmpty()) {
            List<BasketDivisionStage> stages = stageMapper.selectByDivisionId(stage.getDivisionId());
            for (BasketDivisionStage item : stages) {
                if (item.getStageOrder() != null && stage.getStageOrder() != null
                        && item.getStageOrder() > stage.getStageOrder()) {
                    return item;
                }
            }
            return null;
        }
        return stageMapper.selectBySnapshotIdAndStageCode(stage.getSnapshotId(), stage.getNextStageCode());
    }

    private StageTeamEntryTypeEnum mapEntryType(String qualificationType) {
        QualificationTypeEnum type = QualificationTypeEnum.fromCode(qualificationType);
        return switch (type) {
            case OVERALL_TOP_N -> StageTeamEntryTypeEnum.OVERALL_RANK;
            case GROUP_TOP_N -> StageTeamEntryTypeEnum.GROUP_RANK;
            case MATCH_WINNER, CHAMPION -> StageTeamEntryTypeEnum.MATCH_WINNER;
            case MANUAL -> StageTeamEntryTypeEnum.MANUAL;
        };
    }

    private int requireAdvancementCount(BasketDivisionStage stage) {
        if (stage.getAdvancementCount() == null || stage.getAdvancementCount() <= 0) {
            throw new ServiceException("阶段未配置有效晋级名额");
        }
        return stage.getAdvancementCount();
    }

    private String buildQualificationActiveKey(BasketDivisionStageQualification row) {
        return row.getStageId() + ":" + row.getQualificationStatus() + ":"
                + row.getEventTeamId() + ":" + (row.getGroupId() != null ? row.getGroupId() : 0);
    }

    private BasketDivisionStageExecutionListVo toExecutionListVo(BasketDivisionStage stage) {
        BasketDivisionStageExecutionListVo vo = new BasketDivisionStageExecutionListVo();
        vo.setStageId(stage.getId());
        vo.setStageCode(stage.getStageCode());
        vo.setStageName(stage.getStageName());
        vo.setStageOrder(stage.getStageOrder());
        vo.setStageType(stage.getStageType());
        vo.setStageTypeName(V4FormatDisplaySupport.stageTypeLabel(stage.getStageType()));
        vo.setCompetitionFormat(stage.getCompetitionFormat());
        vo.setCompetitionFormatName(V4FormatDisplaySupport.competitionFormatLabel(stage.getCompetitionFormat()));
        vo.setExecutionStatus(stage.getExecutionStatus());
        try {
            vo.setExecutionStatusName(StageExecutionStatusEnum.fromCode(stage.getExecutionStatus()).getDescription());
        } catch (Exception ex) {
            vo.setExecutionStatusName(stage.getExecutionStatus());
        }
        vo.setScheduleBatchId(stage.getScheduleBatchId());
        vo.setParticipantCount(stage.getParticipantCount());
        vo.setQualifiedCount(stage.getQualifiedCount());
        vo.setStartedTime(stage.getStartedTime());
        vo.setCompletedTime(stage.getCompletedTime());
        vo.setConfirmedTime(stage.getConfirmedTime());
        vo.setNextStageCode(stage.getNextStageCode());
        return vo;
    }

    private BasketDivision requireDivision(Long divisionId) {
        BasketDivision division = divisionMapper.selectById(divisionId);
        if (division == null) {
            throw new ServiceException("Division 不存在");
        }
        return division;
    }

    private boolean isStandingStage(BasketDivisionStage stage) {
        if (CompetitionFormatEnum.ROUND_ROBIN.getCode().equals(stage.getCompetitionFormat())) {
            return true;
        }
        return StageTypeEnum.GROUP.getCode().equals(stage.getStageType());
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

    private BasketScheduleBatch requireBatch(Long batchId) {
        BasketScheduleBatch batch = scheduleBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new ServiceException("赛程批次不存在");
        }
        return batch;
    }
}
