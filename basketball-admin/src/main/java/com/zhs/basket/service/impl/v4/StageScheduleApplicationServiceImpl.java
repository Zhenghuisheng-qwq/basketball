package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.domain.v4.BasketStagePublishRecord;
import com.zhs.basket.dto.v4.ScheduleAssignReq;
import com.zhs.basket.dto.v4.ScheduleBatchAssignReq;
import com.zhs.basket.dto.v4.ScheduleRecommendApplyReq;
import com.zhs.basket.dto.v4.ScheduleRescheduleReq;
import com.zhs.basket.enums.v4.PublishStatusEnum;
import com.zhs.basket.enums.v4.ScheduleSourceEnum;
import com.zhs.basket.enums.v4.ScheduleStatusEnum;
import com.zhs.basket.enums.v4.ScheduleUnitTypeEnum;
import com.zhs.basket.enums.v4.StagePublishActionTypeEnum;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageMapper;
import com.zhs.basket.mapper.v4.BasketScheduleBatchMapper;
import com.zhs.basket.mapper.v4.BasketStagePublishRecordMapper;
import com.zhs.basket.service.v4.IStageScheduleApplicationService;
import com.zhs.basket.support.v4.schedule.ScheduleAdjustmentGuard;
import com.zhs.basket.support.v4.schedule.ScheduleAssignmentValidator;
import com.zhs.basket.support.v4.schedule.ScheduleFingerprintSupport;
import com.zhs.basket.support.v4.schedule.ScheduleMutableGuard;
import com.zhs.basket.support.v4.schedule.ScheduleRecommendationStrategy;
import com.zhs.basket.support.v4.schedule.ScheduleRecommendationStrategyFactory;
import com.zhs.basket.support.v4.schedule.ScheduleTimes;
import com.zhs.basket.support.v4.schedule.ScheduleUnitResolver;
import com.zhs.basket.support.v4.schedule.StagePublishContext;
import com.zhs.basket.support.v4.schedule.ValidationIssue;
import com.zhs.basket.support.v4.schedule.ValidationResult;
import com.zhs.basket.vo.v4.ScheduleRecommendationPreviewVo;
import com.zhs.basket.vo.v4.ScheduleUnitView;
import com.zhs.basket.vo.v4.StagePublishRecordVo;
import com.zhs.basket.vo.v4.StageScheduleBoardVo;
import com.zhs.basket.vo.v4.StageScheduleSummaryVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class StageScheduleApplicationServiceImpl implements IStageScheduleApplicationService {

    @Autowired
    private BasketDivisionStageMapper stageMapper;
    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private BasketScheduleBatchMapper scheduleBatchMapper;
    @Autowired
    private ScheduleUnitResolver scheduleUnitResolver;
    @Autowired
    private ScheduleRecommendationStrategyFactory recommendationStrategyFactory;
    @Autowired
    private List<ScheduleAssignmentValidator> assignmentValidators;
    @Autowired
    private BasketStagePublishRecordMapper publishRecordMapper;

    @Override
    public StageScheduleBoardVo listScheduleUnits(Long stageId, String scheduleStatus) {
        BasketScheduleBatch batch = requireActiveBatch(stageId);
        List<ScheduleUnitView> units = scheduleUnitResolver.listUnits(batch);
        if (scheduleStatus != null && !scheduleStatus.isBlank() && !"ALL".equalsIgnoreCase(scheduleStatus)) {
            units = units.stream().filter(u -> scheduleStatus.equals(u.getScheduleStatus())).toList();
        }
        StageScheduleBoardVo board = new StageScheduleBoardVo();
        board.setSummary(toSummary(requireStage(stageId), batch, scheduleUnitResolver.listUnits(batch)));
        board.setUnits(units);
        return board;
    }

    @Override
    public StageScheduleSummaryVo summary(Long stageId) {
        BasketDivisionStage stage = requireStage(stageId);
        BasketScheduleBatch batch = scheduleBatchMapper.selectActiveByStageId(stageId);
        List<ScheduleUnitView> units = batch == null ? List.of() : scheduleUnitResolver.listUnits(batch);
        return toSummary(stage, batch, units);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assign(Long stageId, ScheduleAssignReq req) {
        if (req == null) {
            throw new ServiceException("排期请求不能为空");
        }
        BasketDivisionStage stage = requireStage(stageId);
        BasketScheduleBatch batch = requireActiveBatch(stageId);
        ScheduleMutableGuard.assertNotPublished(batch);
        ScheduleUnitView unit = scheduleUnitResolver.requireUnit(batch, req.getUnitType(), req.getUnitId());
        ScheduleMutableGuard.assertUnitEditable(unit);
        LocalDate date = ScheduleTimes.parseDate(req.getScheduledDate());
        LocalTime time = ScheduleTimes.parseTime(req.getScheduledStartTime());
        List<ScheduleUnitView> proposed = replaceUnitSchedule(scheduleUnitResolver.listUnits(batch), unit, date, time,
                sourceOrManual(req.getScheduleSource()));
        validateAssignment(stage, batch, proposed, date, time);
        scheduleUnitResolver.applySchedule(batch, unit, date, time, sourceOrManual(req.getScheduleSource()));
        bumpScheduleRevision(batch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAssign(Long stageId, ScheduleBatchAssignReq req) {
        if (req == null) {
            throw new ServiceException("批量排期请求不能为空");
        }
        BasketDivisionStage stage = requireStage(stageId);
        BasketScheduleBatch batch = requireActiveBatch(stageId);
        ScheduleMutableGuard.assertNotPublished(batch);
        List<ScheduleUnitView> current = scheduleUnitResolver.listUnits(batch);
        List<ScheduleAssignReq> assignments = new ArrayList<>();
        if (req.getItems() != null && !req.getItems().isEmpty()) {
            for (ScheduleAssignReq item : req.getItems()) {
                ScheduleAssignReq one = new ScheduleAssignReq();
                one.setUnitType(item.getUnitType());
                one.setUnitId(item.getUnitId());
                one.setScheduledDate(item.getScheduledDate() != null ? item.getScheduledDate() : req.getScheduledDate());
                one.setScheduledStartTime(item.getScheduledStartTime() != null ? item.getScheduledStartTime() : req.getScheduledStartTime());
                one.setScheduleSource(sourceOrManual(item.getScheduleSource() != null ? item.getScheduleSource() : req.getScheduleSource()));
                assignments.add(one);
            }
        } else if (req.getRoundNo() != null) {
            for (ScheduleUnitView unit : current) {
                if (Objects.equals(req.getRoundNo(), unit.getRoundNo()) && unit.isRequired()) {
                    ScheduleAssignReq one = new ScheduleAssignReq();
                    one.setUnitType(unit.getUnitType());
                    one.setUnitId(unit.getUnitId());
                    one.setScheduledDate(req.getScheduledDate());
                    one.setScheduledStartTime(req.getScheduledStartTime());
                    one.setScheduleSource(sourceOrManual(req.getScheduleSource()));
                    assignments.add(one);
                }
            }
        }
        if (assignments.isEmpty()) {
            throw new ServiceException("没有可排期的比赛单位");
        }
        List<ScheduleUnitView> proposed = current;
        for (ScheduleAssignReq item : assignments) {
            ScheduleUnitView target = scheduleUnitResolver.requireUnit(batch, item.getUnitType(), item.getUnitId());
            ScheduleMutableGuard.assertUnitEditable(target);
            LocalDate date = ScheduleTimes.parseDate(item.getScheduledDate());
            LocalTime time = ScheduleTimes.parseTime(item.getScheduledStartTime());
            proposed = replaceUnitSchedule(proposed, target, date, time, item.getScheduleSource());
        }
        validateAssignment(stage, batch, proposed, null, null);
        for (ScheduleAssignReq item : assignments) {
            ScheduleUnitView target = scheduleUnitResolver.requireUnit(batch, item.getUnitType(), item.getUnitId());
            scheduleUnitResolver.applySchedule(batch, target,
                    ScheduleTimes.parseDate(item.getScheduledDate()),
                    ScheduleTimes.parseTime(item.getScheduledStartTime()),
                    item.getScheduleSource());
        }
        bumpScheduleRevision(batch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clear(Long stageId, String unitType, Long unitId) {
        BasketScheduleBatch batch = requireActiveBatch(stageId);
        ScheduleMutableGuard.assertNotPublished(batch);
        ScheduleUnitView unit = scheduleUnitResolver.requireUnit(batch, unitType, unitId);
        ScheduleMutableGuard.assertUnitEditable(unit);
        scheduleUnitResolver.applySchedule(batch, unit, null, null, null);
        bumpScheduleRevision(batch);
    }

    @Override
    public ScheduleRecommendationPreviewVo previewRecommend(Long stageId, boolean overwriteScheduled) {
        BasketDivisionStage stage = requireStage(stageId);
        BasketScheduleBatch batch = requireActiveBatch(stageId);
        ScheduleMutableGuard.assertNotPublished(batch);
        BasketDivision division = requireDivision(stage.getDivisionId());
        LocalDate start = requireStartDate(division);
        List<ScheduleUnitView> current = scheduleUnitResolver.listUnits(batch);
        ScheduleRecommendationStrategy strategy = recommendationStrategyFactory.require(stage);
        List<ScheduleUnitView> proposed = strategy.recommend(current, start, overwriteScheduled);
        ScheduleRecommendationPreviewVo vo = new ScheduleRecommendationPreviewVo();
        vo.setRecommendationType(strategy.type().getCode());
        vo.setOverwriteScheduled(overwriteScheduled);
        vo.setUnits(proposed);
        int proposedCount = 0;
        int skipped = 0;
        for (int i = 0; i < current.size(); i++) {
            ScheduleUnitView before = current.get(i);
            ScheduleUnitView after = proposed.stream()
                    .filter(p -> Objects.equals(p.getUnitType(), before.getUnitType()) && Objects.equals(p.getUnitId(), before.getUnitId()))
                    .findFirst().orElse(before);
            boolean changed = !Objects.equals(before.getScheduledDate(), after.getScheduledDate())
                    || !Objects.equals(before.getScheduledStartTime(), after.getScheduledStartTime());
            if (changed) {
                proposedCount++;
            } else if (ScheduleStatusEnum.SCHEDULED.getCode().equals(before.getScheduleStatus())) {
                skipped++;
            }
        }
        vo.setProposedCount(proposedCount);
        vo.setSkippedManualCount(skipped);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyRecommend(Long stageId, ScheduleRecommendApplyReq req) {
        boolean overwrite = req != null && req.isOverwriteScheduled();
        ScheduleRecommendationPreviewVo preview = previewRecommend(stageId, overwrite);
        BasketDivisionStage stage = requireStage(stageId);
        BasketScheduleBatch batch = requireActiveBatch(stageId);
        List<ScheduleUnitView> proposed = preview.getUnits();
        validateAssignment(stage, batch, proposed, null, null);
        List<ScheduleUnitView> current = scheduleUnitResolver.listUnits(batch);
        boolean changed = false;
        for (ScheduleUnitView after : proposed) {
            ScheduleUnitView before = current.stream()
                    .filter(u -> Objects.equals(u.getUnitType(), after.getUnitType()) && Objects.equals(u.getUnitId(), after.getUnitId()))
                    .findFirst().orElse(null);
            if (before == null) {
                continue;
            }
            if (Objects.equals(before.getScheduledDate(), after.getScheduledDate())
                    && Objects.equals(before.getScheduledStartTime(), after.getScheduledStartTime())) {
                continue;
            }
            ScheduleMutableGuard.assertUnitEditable(before);
            LocalDate date = ScheduleTimes.parseDate(after.getScheduledDate());
            LocalTime time = ScheduleTimes.parseTime(after.getScheduledStartTime());
            scheduleUnitResolver.applySchedule(batch, before, date, time, ScheduleSourceEnum.RECOMMENDED.getCode());
            changed = true;
        }
        if (changed) {
            bumpScheduleRevision(batch);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StagePublishRecordVo reschedule(Long stageId, ScheduleRescheduleReq req) {
        if (req == null) {
            throw new ServiceException("排期调整请求不能为空");
        }
        ScheduleAdjustmentGuard.assertReason(req.getReason());
        BasketDivisionStage stage = requireStage(stageId);
        BasketScheduleBatch batch = requireActiveBatch(stageId);
        ScheduleAdjustmentGuard.assertPublished(batch);
        ScheduleUnitView unit = scheduleUnitResolver.requireUnit(batch, req.getUnitType(), req.getUnitId());
        boolean childStarted = ScheduleUnitTypeEnum.MATCH_SERIES.getCode().equals(unit.getUnitType())
                && scheduleUnitResolver.hasStartedChildMatch(unit.getUnitId());
        ScheduleAdjustmentGuard.assertCanAdjust(unit, childStarted);
        LocalDate date = ScheduleTimes.parseDate(req.getScheduledDate());
        LocalTime time = ScheduleTimes.parseTime(req.getScheduledStartTime());
        List<ScheduleUnitView> proposed = replaceUnitSchedule(scheduleUnitResolver.listUnits(batch), unit, date, time,
                ScheduleSourceEnum.MANUAL.getCode());
        validateAssignment(stage, batch, proposed, date, time);
        String oldDate = unit.getScheduledDate();
        String oldTime = unit.getScheduledStartTime();
        scheduleUnitResolver.applySchedule(batch, unit, date, time, ScheduleSourceEnum.MANUAL.getCode());
        bumpScheduleRevision(batch);
        int nextRevision = (batch.getScheduleRevision() == null ? 0 : batch.getScheduleRevision()) + 1;
        batch.setScheduleRevision(nextRevision);
        return persistScheduleAdjust(stage, batch, unit, oldDate, oldTime, req.getScheduledDate(),
                req.getScheduledStartTime(), req.getReason().trim());
    }

    public StageScheduleSummaryVo toSummary(BasketDivisionStage stage, BasketScheduleBatch batch, List<ScheduleUnitView> units) {
        StageScheduleSummaryVo summary = new StageScheduleSummaryVo();
        summary.setStageId(stage.getId());
        int total = 0, scheduled = 0, resolved = 0, unresolved = 0;
        for (ScheduleUnitView unit : units) {
            if (!unit.isRequired()) {
                continue;
            }
            total++;
            if (ScheduleStatusEnum.SCHEDULED.getCode().equals(unit.getScheduleStatus())
                    && unit.getScheduledDate() != null) {
                scheduled++;
            }
            if ("RESOLVED".equals(unit.getParticipantResolutionStatus())) {
                resolved++;
            } else if ("TBD".equals(unit.getParticipantResolutionStatus()) || "UNRESOLVED".equals(unit.getParticipantResolutionStatus())) {
                unresolved++;
            }
        }
        summary.setTotalUnits(total);
        summary.setScheduledUnits(scheduled);
        summary.setUnscheduledUnits(total - scheduled);
        summary.setResolvedUnits(resolved);
        summary.setUnresolvedUnits(unresolved);
        if (batch != null) {
            summary.setStructureBatchId(batch.getId());
            summary.setPublishStatus(batch.getPublishStatus());
            summary.setStructureStatus(batch.getStructureStatus());
            summary.setExecutionVersion(batch.getExecutionVersion());
            summary.setScheduleRevision(batch.getScheduleRevision() == null ? 0 : batch.getScheduleRevision());
        }
        summary.setRecommendationType(recommendationStrategyFactory.resolveType(stage).getCode());
        summary.setCanRecommend("ROUND_ROBIN_STANDARD".equals(summary.getRecommendationType()));
        return summary;
    }

    private void validateAssignment(BasketDivisionStage stage, BasketScheduleBatch batch,
                                    List<ScheduleUnitView> proposed, LocalDate date, LocalTime time) {
        StagePublishContext context = new StagePublishContext();
        context.setStage(stage);
        context.setDivision(requireDivision(stage.getDivisionId()));
        context.setStructureBatch(batch);
        context.setUnits(proposed);
        context.setPublishStatus(batch.getPublishStatus());
        ValidationResult result = new ValidationResult();
        for (ScheduleAssignmentValidator validator : assignmentValidators) {
            validator.validate(context, proposed, date, time, result);
        }
        if (result.hasErrors()) {
            ValidationIssue first = result.errors().get(0);
            throw new ServiceException(first.getMessage());
        }
    }

    private List<ScheduleUnitView> replaceUnitSchedule(List<ScheduleUnitView> units, ScheduleUnitView target,
                                                       LocalDate date, LocalTime time, String source) {
        List<ScheduleUnitView> next = new ArrayList<>();
        for (ScheduleUnitView unit : units) {
            if (Objects.equals(unit.getUnitType(), target.getUnitType()) && Objects.equals(unit.getUnitId(), target.getUnitId())) {
                unit.setScheduledDate(ScheduleTimes.formatDate(date));
                unit.setScheduledStartTime(ScheduleTimes.formatTime(time));
                unit.setScheduleStatus(date == null ? ScheduleStatusEnum.UNSCHEDULED.getCode() : ScheduleStatusEnum.SCHEDULED.getCode());
                unit.setScheduleSource(source);
            }
            next.add(unit);
        }
        return next;
    }

    private void bumpScheduleRevision(BasketScheduleBatch batch) {
        scheduleBatchMapper.incrementScheduleRevision(batch.getId());
    }

    private StagePublishRecordVo persistScheduleAdjust(BasketDivisionStage stage, BasketScheduleBatch batch,
                                                       ScheduleUnitView unit, String oldDate, String oldTime,
                                                       String newDate, String newTime, String reason) {
        Date now = new Date();
        Long operator = 0L;
        try {
            operator = SecurityUtils.getUserId();
        } catch (Exception ignored) {
            operator = 0L;
        }
        BasketStagePublishRecord current = publishRecordMapper.selectCurrentByStageId(stage.getId());
        int publishNo = current != null && current.getPublishNo() != null ? current.getPublishNo() : 1;
        publishRecordMapper.clearCurrentByStageId(stage.getId());

        List<ScheduleUnitView> units = scheduleUnitResolver.listUnits(batch);
        BasketStagePublishRecord record = new BasketStagePublishRecord();
        record.setDivisionId(stage.getDivisionId());
        record.setStageId(stage.getId());
        record.setStructureBatchId(batch.getId());
        record.setStructureExecutionVersion(batch.getExecutionVersion() == null ? 0 : batch.getExecutionVersion());
        record.setRuleVersionId(batch.getRuleVersionId());
        record.setRosterVersionId(batch.getRosterVersionId());
        record.setPairingExecutionId(batch.getPairingExecutionId());
        record.setScheduleRevision(batch.getScheduleRevision() == null ? 0 : batch.getScheduleRevision());
        record.setPublishNo(publishNo);
        record.setActionType(StagePublishActionTypeEnum.SCHEDULE_ADJUST.getCode());
        record.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
        record.setScheduleFingerprint(ScheduleFingerprintSupport.fingerprint(units));
        record.setOperatorId(operator);
        record.setOperationTime(now);
        record.setReason(ScheduleAdjustmentGuard.formatAuditReason(
                unit.getUnitType(), unit.getUnitId(), oldDate, oldTime, newDate, newTime, reason));
        record.setIsCurrent(1);
        record.setCurrentUniqueKey("STAGE:" + stage.getId());
        record.setCreateTime(now);
        record.setUpdateTime(now);
        record.setIsDelete(0);
        if (publishRecordMapper.insert(record) != 1) {
            throw new ServiceException("写入排期调整记录失败");
        }
        StagePublishRecordVo vo = new StagePublishRecordVo();
        vo.setId(record.getId());
        vo.setStageId(record.getStageId());
        vo.setStructureBatchId(record.getStructureBatchId());
        vo.setStructureExecutionVersion(record.getStructureExecutionVersion());
        vo.setScheduleRevision(record.getScheduleRevision());
        vo.setPublishNo(record.getPublishNo());
        vo.setActionType(record.getActionType());
        vo.setPublishStatus(record.getPublishStatus());
        vo.setScheduleFingerprint(record.getScheduleFingerprint());
        vo.setOperatorId(record.getOperatorId());
        vo.setOperationTime(record.getOperationTime());
        vo.setReason(record.getReason());
        vo.setIsCurrent(record.getIsCurrent());
        return vo;
    }

    private String sourceOrManual(String source) {
        if (source == null || source.isBlank()) {
            return ScheduleSourceEnum.MANUAL.getCode();
        }
        return ScheduleSourceEnum.fromCode(source).getCode();
    }

    private LocalDate requireStartDate(BasketDivision division) {
        LocalDate start = ScheduleTimes.toLocalDate(division.getMatchStartTime());
        if (start == null) {
            throw new ServiceException("缺少赛事开始日期，无法生成推荐排期");
        }
        return start;
    }

    private BasketDivisionStage requireStage(Long stageId) {
        BasketDivisionStage stage = stageMapper.selectById(stageId);
        if (stage == null) {
            throw new ServiceException("赛段不存在");
        }
        return stage;
    }

    private BasketDivision requireDivision(Long divisionId) {
        BasketDivision division = divisionMapper.selectById(divisionId);
        if (division == null) {
            throw new ServiceException("Division 不存在");
        }
        return division;
    }

    private BasketScheduleBatch requireActiveBatch(Long stageId) {
        BasketScheduleBatch batch = scheduleBatchMapper.selectActiveByStageId(stageId);
        ScheduleMutableGuard.assertStructureActive(batch);
        return batch;
    }
}
