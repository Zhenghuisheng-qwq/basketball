package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionFormatSnapshot;
import com.zhs.basket.domain.v4.BasketDivisionRosterVersion;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.domain.v4.BasketStagePairingExecution;
import com.zhs.basket.domain.v4.BasketStagePublishRecord;
import com.zhs.basket.dto.v4.StageWithdrawReq;
import com.zhs.basket.enums.v4.PublishStatusEnum;
import com.zhs.basket.enums.v4.StructureStatusEnum;
import com.zhs.basket.enums.v4.StageExecutionStatusEnum;
import com.zhs.basket.enums.v4.StagePublishActionTypeEnum;
import com.zhs.basket.enums.v4.ValidationSeverityEnum;
import com.zhs.basket.mapper.v4.BasketDivisionFormatSnapshotMapper;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketDivisionRosterVersionMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageMapper;
import com.zhs.basket.mapper.v4.BasketScheduleBatchMapper;
import com.zhs.basket.mapper.v4.BasketStagePairingExecutionMapper;
import com.zhs.basket.mapper.v4.BasketStagePublishRecordMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.service.v4.IStagePublishApplicationService;
import com.zhs.basket.support.v4.V4PublicReadCacheEvictEvent;
import com.zhs.basket.support.v4.schedule.ScheduleFingerprintSupport;
import com.zhs.basket.support.v4.schedule.ScheduleUnitResolver;
import com.zhs.basket.support.v4.schedule.StagePublishContext;
import com.zhs.basket.support.v4.schedule.StagePublishStatusTransition;
import com.zhs.basket.support.v4.schedule.StagePublishValidator;
import com.zhs.basket.support.v4.schedule.ValidationIssue;
import com.zhs.basket.support.v4.schedule.ValidationResult;
import com.zhs.basket.vo.v4.ScheduleUnitView;
import com.zhs.basket.vo.v4.StagePublishPreviewVo;
import com.zhs.basket.vo.v4.StagePublishRecordVo;
import com.zhs.basket.vo.v4.StageScheduleSummaryVo;
import com.zhs.basket.vo.v4.ValidationIssueVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class StagePublishApplicationServiceImpl implements IStagePublishApplicationService {

    @Autowired
    private BasketDivisionStageMapper stageMapper;
    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private BasketDivisionFormatSnapshotMapper snapshotMapper;
    @Autowired
    private BasketDivisionRosterVersionMapper rosterVersionMapper;
    @Autowired
    private BasketScheduleBatchMapper scheduleBatchMapper;
    @Autowired
    private BasketStagePairingExecutionMapper pairingExecutionMapper;
    @Autowired
    private BasketStagePublishRecordMapper publishRecordMapper;
    @Autowired
    private BasketV4MatchMapper matchMapper;
    @Autowired
    private ScheduleUnitResolver scheduleUnitResolver;
    @Autowired
    private StageScheduleApplicationServiceImpl scheduleApplicationService;
    @Autowired
    private List<StagePublishValidator> publishValidators;
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public StagePublishPreviewVo preview(Long stageId) {
        StagePublishContext context = buildContext(stageId);
        ValidationResult result = runValidators(context);
        StagePublishPreviewVo vo = new StagePublishPreviewVo();
        vo.setCanPublish(result.canPublish());
        vo.setSummary(context.getScheduleSummary());
        vo.setUnits(context.getUnits());
        vo.setErrors(toIssueVos(result.errors()));
        vo.setWarnings(toIssueVos(result.warnings()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StagePublishRecordVo publish(Long stageId) {
        StagePublishContext context = buildContext(stageId);
        com.zhs.basket.support.v4.DoubleEliminationCapabilityGate.assertCanGenerateSchedule(context.getStage());
        BasketScheduleBatch batch = context.getStructureBatch();
        if (batch != null && PublishStatusEnum.PUBLISHED.getCode().equals(batch.getPublishStatus())) {
            String fingerprint = ScheduleFingerprintSupport.fingerprint(context.getUnits());
            BasketStagePublishRecord current = publishRecordMapper.selectCurrentByStageId(stageId);
            if (current != null && fingerprint.equals(current.getScheduleFingerprint())) {
                return toVo(current);
            }
            throw new ServiceException("当前赛段已发布，如需改期请先撤回");
        }
        ValidationResult result = runValidators(context);
        if (result.hasErrors()) {
            throw new ServiceException(result.errors().get(0).getMessage());
        }
        StagePublishActionTypeEnum action = PublishStatusEnum.WITHDRAWN.getCode().equals(context.getPublishStatus())
                ? StagePublishActionTypeEnum.REPUBLISH
                : StagePublishActionTypeEnum.PUBLISH;
        PublishStatusEnum next = StagePublishStatusTransition.next(context.getPublishStatus(), action);
        return persistPublish(context, action, next, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StagePublishRecordVo withdraw(Long stageId, StageWithdrawReq req) {
        if (req == null || req.getReason() == null || req.getReason().isBlank()) {
            throw new ServiceException("撤回原因必填");
        }
        StagePublishContext context = buildContext(stageId);
        StagePublishStatusTransition.next(context.getPublishStatus(), StagePublishActionTypeEnum.WITHDRAW);
        if (matchMapper.countFinishedByStageId(stageId) > 0) {
            throw new ServiceException("当前赛段已产生正式比赛结果，不能直接撤回重排。");
        }
        return persistPublish(context, StagePublishActionTypeEnum.WITHDRAW, PublishStatusEnum.WITHDRAWN, req.getReason().trim());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StagePublishRecordVo republish(Long stageId) {
        StagePublishContext context = buildContext(stageId);
        StagePublishStatusTransition.next(context.getPublishStatus(), StagePublishActionTypeEnum.REPUBLISH);
        ValidationResult result = runValidators(context);
        if (result.hasErrors()) {
            throw new ServiceException(result.errors().get(0).getMessage());
        }
        return persistPublish(context, StagePublishActionTypeEnum.REPUBLISH, PublishStatusEnum.PUBLISHED, null);
    }

    @Override
    public List<StagePublishRecordVo> listRecords(Long stageId) {
        List<BasketStagePublishRecord> records = publishRecordMapper.selectByStageId(stageId);
        List<StagePublishRecordVo> vos = new ArrayList<>();
        for (BasketStagePublishRecord record : records) {
            vos.add(toVo(record));
        }
        return vos;
    }

    private StagePublishRecordVo persistPublish(StagePublishContext context, StagePublishActionTypeEnum action,
                                                PublishStatusEnum next, String reason) {
        BasketScheduleBatch batch = context.getStructureBatch();
        Date now = new Date();
        Long operator = context.getCurrentOperator();
        batch.setPublishStatus(next.getCode());
        if (next == PublishStatusEnum.PUBLISHED) {
            batch.setPublishTime(now);
            batch.setPublishedBy(operator);
            matchMapper.publishByBatchId(batch.getId());
        } else {
            matchMapper.unpublishByBatchId(batch.getId());
        }
        batch.setUpdateTime(now);
        if (scheduleBatchMapper.updateById(batch) != 1) {
            throw new ServiceException("更新赛段发布状态失败");
        }

        BasketDivisionStage stage = context.getStage();
        if (next == PublishStatusEnum.PUBLISHED) {
            stage.setExecutionStatus(StageExecutionStatusEnum.RUNNING.getCode());
            if (stage.getStartedTime() == null) {
                stage.setStartedTime(now);
            }
        } else if (next == PublishStatusEnum.WITHDRAWN
                && StageExecutionStatusEnum.RUNNING.getCode().equals(stage.getExecutionStatus())) {
            stage.setExecutionStatus(StageExecutionStatusEnum.SCHEDULE_GENERATED.getCode());
        }
        stage.setUpdateTime(now);
        stageMapper.updateExecutionStatus(stage);

        publishRecordMapper.clearCurrentByStageId(stage.getId());
        Integer maxNo = publishRecordMapper.selectMaxPublishNo(stage.getId());
        int publishNo;
        if (action == StagePublishActionTypeEnum.WITHDRAW) {
            publishNo = maxNo == null ? 1 : maxNo;
        } else {
            publishNo = maxNo == null ? 1 : maxNo + 1;
        }

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
        record.setActionType(action.getCode());
        record.setPublishStatus(next.getCode());
        record.setScheduleFingerprint(ScheduleFingerprintSupport.fingerprint(context.getUnits()));
        record.setOperatorId(operator);
        record.setOperationTime(now);
        record.setReason(reason);
        record.setIsCurrent(1);
        record.setCurrentUniqueKey(next == PublishStatusEnum.PUBLISHED ? ("STAGE:" + stage.getId()) : null);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        record.setIsDelete(0);
        if (publishRecordMapper.insert(record) != 1) {
            throw new ServiceException("写入发布记录失败");
        }
        eventPublisher.publishEvent(new V4PublicReadCacheEvictEvent(
                stage.getDivisionId(), null, context.getDivision() == null ? null : context.getDivision().getSeasonId(),
                stage.getId(), null, null));
        return toVo(record);
    }

    private StagePublishContext buildContext(Long stageId) {
        BasketDivisionStage stage = stageMapper.selectById(stageId);
        if (stage == null) {
            throw new ServiceException("赛段不存在");
        }
        BasketDivision division = divisionMapper.selectById(stage.getDivisionId());
        if (division == null) {
            throw new ServiceException("Division 不存在");
        }
        BasketScheduleBatch batch = scheduleBatchMapper.selectActiveByStageId(stageId);
        boolean historicalStale = batch == null
                && scheduleBatchMapper.countByStageIdAndStructureStatus(
                stageId, StructureStatusEnum.STALE.getCode()) > 0;
        BasketDivisionFormatSnapshot rule = snapshotMapper.selectById(stage.getSnapshotId());
        BasketDivisionRosterVersion roster = rosterVersionMapper.selectCurrentLockedByDivisionId(stage.getDivisionId());
        BasketStagePairingExecution pairing = pairingExecutionMapper.selectCurrentByStageId(stageId);
        List<ScheduleUnitView> units = batch == null ? List.of() : scheduleUnitResolver.listUnits(batch);
        StageScheduleSummaryVo summary = scheduleApplicationService.toSummary(stage, batch, units);
        if (rule != null) {
            summary.setRuleStatus(rule.getSnapshotStatus());
        }
        if (roster != null) {
            summary.setRosterStatus(roster.getStatus());
        }
        if (pairing != null) {
            summary.setPairingStatus(pairing.getStatus());
        }
        BasketStagePublishRecord current = publishRecordMapper.selectCurrentByStageId(stageId);
        if (current != null) {
            summary.setPublishNo(current.getPublishNo());
        }
        StagePublishContext context = new StagePublishContext();
        context.setDivision(division);
        context.setStage(stage);
        context.setRuleVersion(rule);
        context.setRosterVersion(roster);
        context.setStructureBatch(batch);
        context.setHistoricalStaleStructure(historicalStale);
        context.setPairingExecution(pairing);
        context.setScheduleSummary(summary);
        context.setUnits(units);
        context.setPublishStatus(batch == null ? PublishStatusEnum.DRAFT.getCode() : batch.getPublishStatus());
        try {
            context.setCurrentOperator(SecurityUtils.getUserId());
        } catch (Exception ex) {
            context.setCurrentOperator(0L);
        }
        List<BasketDivisionStage> stages = stageMapper.selectByDivisionId(stage.getDivisionId());
        context.setFirstStage(stages.isEmpty() || stages.get(0).getId().equals(stage.getId()));
        return context;
    }

    private ValidationResult runValidators(StagePublishContext context) {
        ValidationResult result = new ValidationResult();
        for (StagePublishValidator validator : publishValidators) {
            validator.validate(context, result);
        }
        return result;
    }

    private List<ValidationIssueVo> toIssueVos(List<ValidationIssue> issues) {
        List<ValidationIssueVo> vos = new ArrayList<>();
        for (ValidationIssue issue : issues) {
            ValidationIssueVo vo = new ValidationIssueVo();
            vo.setSeverity(issue.getSeverity() == null ? ValidationSeverityEnum.ERROR.getCode() : issue.getSeverity().getCode());
            vo.setCode(issue.getCode());
            vo.setMessage(issue.getMessage());
            vo.setUnitType(issue.getUnitType());
            vo.setUnitId(issue.getUnitId());
            vos.add(vo);
        }
        return vos;
    }

    private StagePublishRecordVo toVo(BasketStagePublishRecord record) {
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
}
