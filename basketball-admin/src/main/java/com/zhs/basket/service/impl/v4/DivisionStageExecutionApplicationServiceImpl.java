package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.BasketTeam;
import com.zhs.basket.domain.v4.*;
import com.zhs.basket.dto.v4.BasketStageManualRankReq;
import com.zhs.basket.enums.v4.*;
import com.zhs.basket.mapper.BasketTeamMapper;
import com.zhs.basket.mapper.v4.*;
import com.zhs.basket.service.v4.IBasketV4StructureVersionService;
import com.zhs.basket.service.v4.IStagePairingExecutionApplicationService;
import com.zhs.basket.support.v4.bracket.GroupRankPlacementSupport;
import com.zhs.basket.support.v4.stage.SingleEliminationStageScheduleStrategy;
import com.zhs.basket.service.v4.IDivisionStageExecutionApplicationService;
import com.zhs.basket.service.v4.IStagePublishApplicationService;
import com.zhs.basket.support.v4.stage.PlayoffBracketSupport;
import com.zhs.basket.support.v4.ByeQualificationService;
import com.zhs.basket.support.v4.BestOfCapabilityGate;
import com.zhs.basket.support.v4.DoubleEliminationCapabilityGate;
import com.zhs.basket.support.v4.MatchSeriesQualificationSyncService;
import com.zhs.basket.support.v4.StageFormatSpecification;
import com.zhs.basket.support.v4.V4FormatDisplaySupport;
import com.zhs.basket.support.v4.roster.TeamRosterSupport;
import com.zhs.basket.support.v4.stage.EliminationStageSupport;
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
    private BasketDivisionFormatSnapshotMapper snapshotMapper;
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
    private BasketMatchSeriesMapper matchSeriesMapper;
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
    @Autowired
    private IStagePublishApplicationService stagePublishApplicationService;
    @Autowired
    private ByeQualificationService byeQualificationService;
    @Autowired
    private BasketTeamMapper teamMapper;
    @Autowired
    private IBasketV4StructureVersionService structureVersionService;
    @Autowired
    private TeamRosterSupport teamRosterSupport;
    @Autowired
    private IStagePairingExecutionApplicationService pairingExecutionApplicationService;

    @Override
    public List<BasketDivisionStageExecutionListVo> listStages(Long divisionId) {
        requireDivision(divisionId);
        List<BasketDivisionStage> stages = listCurrentRuleStages(divisionId);
        List<BasketDivisionStageExecutionListVo> result = new ArrayList<>();
        for (BasketDivisionStage stage : stages) {
            result.add(toExecutionListVo(stage));
        }
        return result;
    }

    private List<BasketDivisionStage> listCurrentRuleStages(Long divisionId) {
        BasketDivisionFormatSnapshot snapshot = snapshotMapper.selectActiveByDivisionId(divisionId);
        if (snapshot != null) {
            return stageMapper.selectBySnapshotId(snapshot.getId());
        }
        return stageMapper.selectByDivisionId(divisionId);
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
            List<BasketEventTeam> eventTeams = teamRosterSupport.selectFormalEventTeams(division.getId());
            if (PlayoffBracketSupport.isPlayoffBracket(stage.getPairingRule())) {
                eventTeams = sortEventTeamsBySourceTeamCreateTime(eventTeams);
            }
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
            boolean playoffInherit = PlayoffBracketSupport.isPlayoffBracket(stage.getPairingRule())
                    || PairingRuleEnum.HIGH_VS_LOW.getCode().equals(stage.getPairingRule());
            boolean winnerQuals = !confirmed.isEmpty() && confirmed.stream().allMatch(q -> {
                StageTeamEntryTypeEnum et = mapEntryType(q.getQualificationType());
                return et == StageTeamEntryTypeEnum.MATCH_WINNER
                        || et == StageTeamEntryTypeEnum.SERIES_WINNER
                        || et == StageTeamEntryTypeEnum.BYE_ADVANCE;
            });
            boolean tctSeed = playoffInherit
                    && PlayoffBracketSupport.useTeamCreateTimeSeed(stage.getStageCode(), false, winnerQuals);
            Map<Long, Integer> tctSeedByEventTeam = new HashMap<>();
            if (tctSeed) {
                List<BasketEventTeam> advancing = confirmed.stream()
                        .map(q -> eventTeamMapper.selectById(q.getEventTeamId()))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                List<BasketEventTeam> byCreateTime = sortEventTeamsBySourceTeamCreateTime(advancing);
                int seq = 1;
                for (BasketEventTeam et : byCreateTime) {
                    tctSeedByEventTeam.put(et.getId(), seq++);
                }
            }
            for (BasketDivisionStageQualification qualification : confirmed) {
                if (existingEventTeamIds.contains(qualification.getEventTeamId())) {
                    continue;
                }
                StageTeamEntryTypeEnum entryType = mapEntryType(qualification.getQualificationType());
                int seedNo;
                if (tctSeed) {
                    seedNo = tctSeedByEventTeam.getOrDefault(qualification.getEventTeamId(), nextSeed++);
                } else if (playoffInherit && qualification.getSourceRank() != null) {
                    seedNo = qualification.getSourceRank();
                } else {
                    seedNo = nextSeed++;
                }
                toInsert.add(buildStageTeam(stage, qualification.getEventTeamId(),
                        previousStage.getId(), qualification.getGroupId(), qualification.getSourceRank(),
                        seedNo, entryType.getCode(), now));
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
    public List<BasketStageTeamItemVo> listStageTeams(Long stageId) {
        requireStage(stageId);
        List<BasketDivisionStageTeam> teams = stageTeamMapper.selectByStageId(stageId);
        List<BasketStageTeamItemVo> result = new ArrayList<>();
        for (BasketDivisionStageTeam team : teams) {
            BasketStageTeamItemVo vo = new BasketStageTeamItemVo();
            vo.setEventTeamId(team.getEventTeamId());
            vo.setSeedNo(team.getSeedNo());
            BasketEventTeam eventTeam = eventTeamMapper.selectById(team.getEventTeamId());
            if (eventTeam != null) {
                vo.setTeamName(eventTeam.getTeamNameSnapshot());
            }
            result.add(vo);
        }
        result.sort(Comparator
                .comparing(BasketStageTeamItemVo::getSeedNo, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(BasketStageTeamItemVo::getEventTeamId, Comparator.nullsLast(Long::compareTo)));
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
        BasketDivision division = divisionMapper.selectByIdForUpdate(stage.getDivisionId());
        if (division == null) {
            throw new ServiceException("Division 不存在");
        }
        BestOfCapabilityGate.assertCanGenerateSchedule(stage);
        DoubleEliminationCapabilityGate.assertCanGenerateSchedule(stage);
        schedulePrerequisiteValidator.validateBeforeGenerateSchedule(division, stage, isFirstStage(stage));

        if (GroupRankPlacementSupport.usesGroupRankPlacement(stage, findPreviousStage(stage))) {
            var pairing = pairingExecutionApplicationService.getCurrent(stageId);
            if (pairing.getExecutionId() == null || !"CONFIRMED".equals(pairing.getStatus())) {
                throw new ServiceException("请先确认 GROUP_CROSS / MANUAL 占位后再生成淘汰结构");
            }
        } else {
            initializeStageTeams(stageId);
            stage = requireStage(stageId);
            assertPlayoffQualificationReady(stage);
        }

        if (scheduleBatchMapper.selectPublishedByStageId(stageId) != null) {
            throw new ServiceException("当前赛段已发布，需要先撤回后再调整");
        }
        if (matchMapper.countFinishedByStageId(stageId) > 0) {
            throw new ServiceException("赛事已产生正式比赛结果，不能重建结构");
        }
        String status = stage.getExecutionStatus();
        if (StageExecutionStatusEnum.RUNNING.getCode().equals(status)
                || StageExecutionStatusEnum.RESULT_PENDING_CONFIRM.getCode().equals(status)
                || StageExecutionStatusEnum.COMPLETED.getCode().equals(status)) {
            throw new ServiceException("当前阶段状态不允许生成赛程");
        }

        StageExecutionContext context = buildExecutionContext(stage);
        BasketScheduleBatch active = scheduleBatchMapper.selectActiveByStageId(stageId);
        if (active != null && sameStructureBinding(active, context)
                && (PublishStatusEnum.DRAFT.getCode().equals(active.getPublishStatus())
                    || PublishStatusEnum.WITHDRAWN.getCode().equals(active.getPublishStatus()))) {
            return active.getId();
        }
        if (active != null && (PublishStatusEnum.DRAFT.getCode().equals(active.getPublishStatus())
                || PublishStatusEnum.WITHDRAWN.getCode().equals(active.getPublishStatus()))) {
            structureVersionService.markStale(active.getId());
        }

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
        stagePublishApplicationService.publish(stageId);
    }

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
        matchSeriesMapper.softDeleteDraftByBatchId(batch.getId());
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
        if (matchSeriesMapper.countByStageId(stageId) > 0) {
            if (matchSeriesMapper.countIncompleteByStageId(stageId) > 0) {
                return;
            }
        } else {
            int pending = matchMapper.countPendingByStageId(stageId);
            if (pending > 0) {
                return;
            }
            int finished = matchMapper.countFinishedByStageId(stageId);
            if (finished <= 0) {
                return;
            }
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

        if (supportsStanding(stage)
                && StageExecutionStatusEnum.RUNNING.getCode().equals(stage.getExecutionStatus())) {
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
            // GROUP_CROSS knockout may already be PUBLISHED with TBD placeholders.
            // Natural resolution still materializes confirmed qualifiers onto stage_team
            // without rebuilding structure or changing PublishNo.
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
            case DOUBLE_ELIMINATION -> throw new ServiceException(DoubleEliminationCapabilityGate.RESERVED_MESSAGE);
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
        AdvancementTypeEnum adv = AdvancementTypeEnum.fromCode(stage.getAdvancementType());
        if (nextStage != null && adv != AdvancementTypeEnum.WINNER_ADVANCE
                && adv != AdvancementTypeEnum.SERIES_WINNER) {
            throw new ServiceException("淘汰赛非最终阶段晋级方式必须为 WINNER_ADVANCE");
        }

        if (matchSeriesMapper.countByStageId(stage.getId()) > 0) {
            return buildSeriesEliminationPreview(stage, nextStage, targetStageId, now);
        }

        List<BasketV4Match> finished;
        if (EliminationStageSupport.isSingleRoundNonFinalStage(stage)) {
            finished = matchMapper.selectByStageAndRound(stage.getId(), 1).stream()
                    .filter(m -> m.getWinnerEventTeamId() != null
                            && (MatchStatusEnum.FINISHED.getCode().equals(m.getMatchStatus())
                            || MatchStatusEnum.FORFEITED.getCode().equals(m.getMatchStatus())))
                    .collect(Collectors.toList());
        } else {
            finished = matchMapper.selectFinishedByStageId(stage.getId());
            if (finished.isEmpty()) {
                throw new ServiceException("阶段尚无已结束比赛，无法预览晋级");
            }
            int maxRound = finished.stream()
                    .map(BasketV4Match::getRoundNo)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(0);
            finished = finished.stream()
                    .filter(match -> Objects.equals(match.getRoundNo(), maxRound))
                    .filter(match -> match.getWinnerEventTeamId() != null)
                    .collect(Collectors.toList());
        }

        if (finished.isEmpty()) {
            throw new ServiceException("阶段尚无已结束比赛，无法预览晋级");
        }

        List<BasketDivisionStageQualification> result = new ArrayList<>();
        for (BasketV4Match match : finished) {
            QualificationTypeEnum type = nextStage == null
                    ? QualificationTypeEnum.CHAMPION : QualificationTypeEnum.MATCH_WINNER;
            BasketDivisionStageQualification row = new BasketDivisionStageQualification();
            row.setDivisionId(stage.getDivisionId());
            row.setStageId(stage.getId());
            row.setEventTeamId(match.getWinnerEventTeamId());
            row.setQualificationType(type.getCode());
            // 固定淘汰树：sourceRank = bracketSlot+1，供下一 Stage 相邻继承，禁止按完赛顺序重排
            row.setSourceRank(match.getBracketSlot() != null ? match.getBracketSlot() + 1 : null);
            row.setTargetStageId(targetStageId);
            row.setQualificationStatus(QualificationRecordStatusEnum.PREVIEW.getCode());
            row.setActiveUniqueKey(buildQualificationActiveKey(row));
            row.setCreateTime(now);
            row.setUpdateTime(now);
            row.setIsDelete(NOT_DELETED);
            result.add(row);
        }
        byeQualificationService.appendByePreviewIfNeeded(stage, stageTeamMapper.selectByStageId(stage.getId()), result, now);
        return result;
    }

    private List<BasketDivisionStageQualification> buildSeriesEliminationPreview(BasketDivisionStage stage,
                                                                                 BasketDivisionStage nextStage,
                                                                                 Long targetStageId,
                                                                                 Date now) {
        List<BasketMatchSeries> completed = matchSeriesMapper.selectCompletedByStageId(stage.getId());
        if (completed.isEmpty()) {
            throw new ServiceException("阶段尚无已结束系列赛，无法预览晋级");
        }
        List<BasketDivisionStageQualification> result = new ArrayList<>();
        for (BasketMatchSeries series : completed) {
            if (series.getWinnerEventTeamId() == null) {
                continue;
            }
            QualificationTypeEnum type = nextStage == null
                    ? QualificationTypeEnum.CHAMPION : QualificationTypeEnum.SERIES_WINNER;
            BasketDivisionStageQualification row = new BasketDivisionStageQualification();
            row.setDivisionId(stage.getDivisionId());
            row.setStageId(stage.getId());
            row.setEventTeamId(series.getWinnerEventTeamId());
            row.setQualificationType(type.getCode());
            // 与 MatchSeriesQualificationSyncService 一致：sourceRank = bracketSlot+1
            row.setSourceRank(series.getBracketSlot() != null ? series.getBracketSlot() + 1 : null);
            row.setTargetStageId(targetStageId);
            row.setSourceSeriesId(series.getId());
            row.setQualificationStatus(QualificationRecordStatusEnum.PREVIEW.getCode());
            row.setActiveUniqueKey(MatchSeriesQualificationSyncService.buildSeriesQualificationKey(
                    series.getId(), QualificationRecordStatusEnum.PREVIEW.getCode()));
            row.setCreateTime(now);
            row.setUpdateTime(now);
            row.setIsDelete(NOT_DELETED);
            result.add(row);
        }
        if (result.isEmpty()) {
            throw new ServiceException("阶段尚无已结束系列赛，无法预览晋级");
        }
        byeQualificationService.appendByePreviewIfNeeded(stage, stageTeamMapper.selectByStageId(stage.getId()), result, now);
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
            if (row.getGroupId() != null) {
                BasketDivisionGroup group = groupMapper.selectById(row.getGroupId());
                if (group != null) {
                    item.setGroupCode(group.getGroupCode());
                    item.setGroupName(group.getGroupName());
                }
            }
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
        context.setVersionContext(structureVersionService.resolveForGenerate(division.getId()));
        if (SingleEliminationStageScheduleStrategy.requiresConfirmedDraw(stage)
                || GroupRankPlacementSupport.usesGroupRankPlacement(stage, findPreviousStage(stage))) {
            var plan = pairingExecutionApplicationService.loadConfirmedPlan(stage);
            context.setBracketSlotPlan(plan);
            var vo = pairingExecutionApplicationService.getCurrent(stage.getId());
            context.setPairingExecutionId(vo.getExecutionId());
            if (context.getVersionContext() != null) {
                context.getVersionContext().setPairingExecutionId(vo.getExecutionId());
            }
        }

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

    /**
     * 季后赛默认种子：EventTeam → sourceTeamId → basket_team.create_time ASC, id ASC。
     */
    private List<BasketEventTeam> sortEventTeamsBySourceTeamCreateTime(List<BasketEventTeam> eventTeams) {
        Map<Long, BasketTeam> teamCache = new HashMap<>();
        return eventTeams.stream()
                .sorted(Comparator
                        .comparing((BasketEventTeam et) -> {
                            BasketTeam team = resolveSourceTeam(et, teamCache);
                            return team != null && team.getCreateTime() != null
                                    ? team.getCreateTime().getTime() : Long.MAX_VALUE;
                        })
                        .thenComparing(et -> {
                            BasketTeam team = resolveSourceTeam(et, teamCache);
                            return team != null && team.getId() != null ? team.getId() : Long.MAX_VALUE;
                        })
                        .thenComparing(BasketEventTeam::getId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());
    }

    private BasketTeam resolveSourceTeam(BasketEventTeam eventTeam, Map<Long, BasketTeam> cache) {
        if (eventTeam == null || eventTeam.getSourceTeamId() == null) {
            return null;
        }
        return cache.computeIfAbsent(eventTeam.getSourceTeamId(), teamMapper::selectById);
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
        List<BasketDivisionStageTeam> existing = stageTeamMapper.selectByStageId(stage.getId());
        if (GroupRankPlacementSupport.allowsNaturalResolutionTeamSync(stage, existing.isEmpty())) {
            return;
        }
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

    private boolean sameStructureBinding(BasketScheduleBatch batch, StageExecutionContext context) {
        Long ruleId = context.getVersionContext() == null ? null : context.getVersionContext().getRuleVersionId();
        Long rosterId = context.getVersionContext() == null ? null : context.getVersionContext().getRosterVersionId();
        Long pairingId = context.getPairingExecutionId();
        return Objects.equals(batch.getRuleVersionId(), ruleId)
                && Objects.equals(batch.getRosterVersionId(), rosterId)
                && Objects.equals(batch.getPairingExecutionId(), pairingId);
    }

    private void assertPlayoffQualificationReady(BasketDivisionStage stage) {
        if (isFirstStage(stage)) {
            return;
        }
        BasketDivisionStage previous = findPreviousStage(stage);
        if (previous == null) {
            return;
        }
        List<BasketDivisionStageQualification> confirmed = qualificationMapper.selectConfirmedByStageId(previous.getId());
        if (confirmed.isEmpty()) {
            throw new ServiceException("晋级尚未确认，无法生成淘汰结构");
        }
        Integer slots = previous.getAdvancementCount();
        if (slots != null && slots > 0 && confirmed.size() < slots) {
            throw new ServiceException("季后赛名额大于已确认晋级队数，无法生成结构");
        }
    }

    private boolean supportsStanding(BasketDivisionStage stage) {
        CompetitionFormatEnum format = CompetitionFormatEnum.fromCode(stage.getCompetitionFormat());
        return format == CompetitionFormatEnum.ROUND_ROBIN;
    }

    private boolean isGroupStage(BasketDivisionStage stage) {
        return StageFormatSpecification.isGrouped(stage);
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
            case SERIES_WINNER -> StageTeamEntryTypeEnum.SERIES_WINNER;
            case BYE_ADVANCE -> StageTeamEntryTypeEnum.BYE_ADVANCE;
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
        BasketScheduleBatch activeBatch = scheduleBatchMapper.selectActiveByStageId(stage.getId());
        if (activeBatch != null) {
            vo.setPublishStatus(activeBatch.getPublishStatus());
            vo.setStructureStatus(activeBatch.getStructureStatus());
            vo.setExecutionVersion(activeBatch.getExecutionVersion());
            vo.setScheduleRevision(activeBatch.getScheduleRevision());
        }
        vo.setParticipantCount(stage.getParticipantCount());
        vo.setQualifiedCount(stage.getQualifiedCount());
        vo.setStartedTime(stage.getStartedTime());
        vo.setCompletedTime(stage.getCompletedTime());
        vo.setConfirmedTime(stage.getConfirmedTime());
        vo.setNextStageCode(stage.getNextStageCode());
        vo.setRoundRobinCycles(stage.getRoundRobinCycles());
        vo.setBestOf(stage.getBestOf());
        vo.setSemifinalBestOf(stage.getSemifinalBestOf());
        vo.setFinalBestOf(stage.getFinalBestOf());
        vo.setPairingRule(stage.getPairingRule());
        vo.setSeedRule(stage.getSeedRule());
        vo.setDrawRule(stage.getDrawRule());
        vo.setByeRule(stage.getByeRule());
        vo.setAdvancementCount(stage.getAdvancementCount());
        vo.setAdvancementType(stage.getAdvancementType());
        try {
            var pairingVo = pairingExecutionApplicationService.getCurrent(stage.getId());
            vo.setHasUnresolvedPlaceholders(Boolean.TRUE.equals(pairingVo.getHasUnresolvedPlaceholders()));
            vo.setParticipantResolved(Boolean.TRUE.equals(pairingVo.getParticipantResolved()));
        } catch (RuntimeException ignored) {
            vo.setHasUnresolvedPlaceholders(false);
            vo.setParticipantResolved(false);
        }
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
        return CompetitionFormatEnum.ROUND_ROBIN.getCode().equals(stage.getCompetitionFormat());
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
