package com.zhs.basket.service.impl;

import com.zhs.basket.domain.*;
import com.zhs.basket.enums.DisciplinePenaltyCodeEnum;
import com.zhs.basket.enums.DisciplineRecordStatusEnum;
import com.zhs.basket.enums.DisciplineStatusEnum;
import com.zhs.basket.enums.DisciplineTargetTypeEnum;
import com.zhs.basket.mapper.*;
import com.zhs.basket.query.DisciplineQuery;
import com.zhs.basket.req.AuditFieldInput;
import com.zhs.basket.req.DisciplineCreateReq;
import com.zhs.basket.req.DisciplineRevokeReq;
import com.zhs.basket.service.IBasketAnnouncementService;
import com.zhs.basket.service.IBasketDisciplineService;
import com.zhs.basket.service.IPenaltyExecutionService;
import com.zhs.basket.support.DisciplineAnnouncementDraftSupport;
import com.zhs.basket.support.DisciplinePenaltyTargetCompatibilityPolicy;
import com.zhs.basket.support.restriction.DisciplineRestrictionAggregator;
import com.zhs.basket.req.AnnouncementAddReq;
import com.zhs.basket.req.DisciplineAnnouncementDraftReq;
import com.zhs.basket.vo.DisciplineAnnouncementDraftVo;
import com.zhs.basket.vo.PenaltyExecutionResultVo;
import com.zhs.basket.support.BasketBusinessAuditWriter;
import com.zhs.basket.vo.DisciplineDetailVo;
import com.zhs.basket.vo.DisciplineListVo;
import com.zhs.basket.vo.DisciplineSelectorOptionVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class BasketDisciplineServiceImpl implements IBasketDisciplineService {

    private static final Integer NOT_DELETED = 0;

    @Autowired private BasketDisciplineRecordMapper disciplineRecordMapper;
    @Autowired private BasketDisciplineSelectorMapper disciplineSelectorMapper;
    @Autowired private DisciplineRestrictionAggregator restrictionAggregator;
    @Autowired private BasketPointAdjustmentMapper pointAdjustmentMapper;
    @Autowired private BasketDisciplineSuspensionLogMapper suspensionLogMapper;
    @Autowired private BasketMatchMapper matchMapper;
    @Autowired private com.zhs.basket.mapper.v4.BasketSeasonMapper seasonMapper;
    @Autowired private BasketBusinessAuditWriter auditWriter;
    @Autowired private IPenaltyExecutionService penaltyExecutionService;
    @Autowired private BasketDisciplineExecutionDetailMapper executionDetailMapper;
    @Autowired private IBasketAnnouncementService announcementService;
    @Autowired private BasketAnnouncementMapper announcementMapper;

    @Override
    public List<DisciplineListVo> list(DisciplineQuery query) {
        List<DisciplineListVo> rows = disciplineRecordMapper.selectList(query != null ? query : new DisciplineQuery());
        Date now = new Date();
        if (rows != null) {
            for (DisciplineListVo row : rows) {
                fillListDisplay(row, now);
            }
        }
        return rows;
    }

    @Override
    public DisciplineDetailVo detail(Long id) {
        DisciplineDetailVo detail = disciplineRecordMapper.selectDetailById(id);
        if (detail == null) {
            throw new ServiceException("处罚记录不存在");
        }
        fillEventImpact(detail);
        fillAnnouncementStatus(detail);
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(DisciplineCreateReq req) {
        validateCreateReq(req);
        sanitizeCreateTargets(req);
        if (StringUtils.isNotEmpty(req.getIdempotencyKey())
                && disciplineRecordMapper.countByIdempotencyKey(req.getIdempotencyKey()) > 0) {
            throw new ServiceException("重复提交处罚记录");
        }
        if (req.getSeasonId() != null) {
            com.zhs.basket.domain.v4.BasketSeason season = seasonMapper.selectById(req.getSeasonId());
            if (season == null) {
                throw new ServiceException("赛季不存在");
            }
        }
        Date now = new Date();
        PenaltyParams params = normalizeAndValidatePenaltyParams(req, now);
        BasketDisciplineRecord row = new BasketDisciplineRecord();
        row.setRecordNo("DIS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        row.setTargetType(StringUtils.isNotEmpty(req.getTargetType()) ? req.getTargetType() : DisciplineTargetTypeEnum.PLAYER.getCode());
        row.setSeasonId(req.getSeasonId());
        row.setMatchId(req.getMatchId());
        row.setTeamId(req.getTeamId());
        row.setEventTeamId(req.getEventTeamId());
        row.setPlayerId(req.getPlayerId());
        row.setMemberId(req.getMemberId());
        row.setDisciplineType(req.getDisciplineType());
        row.setPenaltyCode(req.getPenaltyCode());
        row.setTitle(req.getTitle());
        row.setReasonText(req.getReasonText());
        row.setPenaltyContent(StringUtils.isNotEmpty(req.getPenaltyContent()) ? req.getPenaltyContent() : req.getTitle());
        row.setMatchSuspensionCount(params.matchSuspensionCount);
        row.setMatchSuspensionRemaining(params.matchSuspensionCount);
        row.setPointDeduction(params.pointDeduction);
        row.setDisciplineStatus(DisciplineStatusEnum.INACTIVE.getCode());
        row.setRecordStatus(DisciplineRecordStatusEnum.DRAFT.name());
        if (DisciplinePenaltyCodeEnum.EVENT_DISQUALIFICATION.name().equals(req.getPenaltyCode())) {
            row.setExecutionStatus(com.zhs.basket.enums.PenaltyExecutionStatusEnum.PENDING.name());
        }
        row.setEffectiveStartTime(params.effectiveStartTime);
        row.setEffectiveEndTime(params.effectiveEndTime);
        row.setDecisionBy(SecurityUtils.getUserId());
        row.setDecisionTime(now);
        row.setSourceType("ADMIN");
        row.setIdempotencyKey(req.getIdempotencyKey());
        row.setCreateTime(now);
        row.setUpdateTime(now);
        row.setIsDelete(NOT_DELETED);
        disciplineRecordMapper.insert(row);
        auditWriter.writeAudit("DISCIPLINE", "DISCIPLINE_RECORD", row.getId(), "CREATE", "创建处罚记录", null, null);
        return row.getId();
    }

    @Override
    public boolean needsEventExecution(Long id) {
        BasketDisciplineRecord record = requireRecord(id);
        return DisciplinePenaltyCodeEnum.EVENT_DISQUALIFICATION.name().equals(record.getPenaltyCode())
                && DisciplineTargetTypeEnum.EVENT_TEAM.getCode().equalsIgnoreCase(record.getTargetType())
                && record.getEventTeamId() != null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activate(Long id) {
        BasketDisciplineRecord record = requireRecord(id);
        DisciplinePenaltyTargetCompatibilityPolicy.validateRecord(record);
        String fromStatus = record.getRecordStatus();
        if (!DisciplineRecordStatusEnum.DRAFT.name().equals(fromStatus)
                && !DisciplineRecordStatusEnum.APPROVED.name().equals(fromStatus)
                && !DisciplineRecordStatusEnum.PENDING.name().equals(fromStatus)) {
            throw new ServiceException("当前状态不可生效");
        }
        if (disciplineRecordMapper.updateRecordStatusIfMatch(id, fromStatus, DisciplineRecordStatusEnum.ACTIVE.name(),
                DisciplineStatusEnum.ACTIVE.getCode(), SecurityUtils.getUserId(), null) != 1) {
            throw new ServiceException("处罚生效失败，可能已被其他管理员处理");
        }
        record.setRecordStatus(DisciplineRecordStatusEnum.ACTIVE.name());
        record.setDisciplineStatus(DisciplineStatusEnum.ACTIVE.getCode());
        restrictionAggregator.recompute(record);
        writePointAdjustmentIfNeeded(record);
        List<AuditFieldInput> fields = Arrays.asList(
                field("recordStatus", "记录状态", fromStatus, DisciplineRecordStatusEnum.ACTIVE.name()),
                field("disciplineStatus", "处罚状态", String.valueOf(DisciplineStatusEnum.INACTIVE.getCode()),
                        String.valueOf(DisciplineStatusEnum.ACTIVE.getCode())));
        auditWriter.writeAudit("DISCIPLINE", "DISCIPLINE_RECORD", id, "ACTIVATE", "处罚生效", null, fields);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long id, DisciplineRevokeReq req) {
        if (req == null || StringUtils.isEmpty(req.getRevokeReason())) {
            throw new ServiceException("撤销原因不能为空");
        }
        BasketDisciplineRecord record = requireRecord(id);
        if (!DisciplineRecordStatusEnum.ACTIVE.name().equals(record.getRecordStatus())) {
            throw new ServiceException("仅生效中的处罚可撤销");
        }
        if (disciplineRecordMapper.updateRecordStatusIfMatch(id, DisciplineRecordStatusEnum.ACTIVE.name(),
                DisciplineRecordStatusEnum.REVOKED.name(), DisciplineStatusEnum.INACTIVE.getCode(),
                SecurityUtils.getUserId(), req.getRevokeReason()) != 1) {
            throw new ServiceException("处罚撤销失败，可能已被其他管理员处理");
        }
        record.setRecordStatus(DisciplineRecordStatusEnum.REVOKED.name());
        record.setDisciplineStatus(DisciplineStatusEnum.INACTIVE.getCode());
        restrictionAggregator.recompute(record);
        auditWriter.writeAudit("DISCIPLINE", "DISCIPLINE_RECORD", id, "REVOKE", "撤销处罚", req.getRevokeReason(), null);
        if (DisciplinePenaltyCodeEnum.EVENT_DISQUALIFICATION.name().equals(record.getPenaltyCode())) {
            penaltyExecutionService.restoreEligibility(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void consumeMatchSuspension(Long matchId, Long memberId) {
        if (matchId == null || memberId == null) {
            throw new ServiceException("比赛与用户不能为空");
        }
        BasketMatch match = matchMapper.selectById(matchId);
        if (match == null) {
            throw new ServiceException("比赛不存在");
        }
        String idempotencyKey = "MATCH:" + matchId + ":MEMBER:" + memberId;
        if (suspensionLogMapper.countByIdempotencyKey(idempotencyKey) > 0) {
            return;
        }
        Date now = new Date();
        BasketDisciplineRecord record = disciplineRecordMapper.selectActiveMatchSuspension(match.getSeasonId(), memberId, now);
        if (record == null) {
            return;
        }
        int remaining = record.getMatchSuspensionRemaining() != null ? record.getMatchSuspensionRemaining() : 0;
        if (remaining <= 0) {
            return;
        }
        int nextRemaining = remaining - 1;
        disciplineRecordMapper.updateMatchSuspensionRemaining(record.getId(), nextRemaining);
        record.setMatchSuspensionRemaining(nextRemaining);
        restrictionAggregator.recompute(record);
        BasketDisciplineSuspensionLog log = new BasketDisciplineSuspensionLog();
        log.setDisciplineId(record.getId());
        log.setMatchId(matchId);
        log.setMemberId(memberId);
        log.setConsumeCount(1);
        log.setIdempotencyKey(idempotencyKey);
        log.setCreateTime(now);
        log.setUpdateTime(now);
        log.setIsDelete(NOT_DELETED);
        suspensionLogMapper.insert(log);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DisciplineAnnouncementDraftVo generateAnnouncementDraft(Long id, DisciplineAnnouncementDraftReq req) {
        DisciplineDetailVo detail = detail(id);
        if (detail.getAnnouncementId() != null) {
            BasketAnnouncement existing = announcementMapper.selectById(detail.getAnnouncementId());
            if (existing != null && (existing.getIsDelete() == null || existing.getIsDelete() == 0)) {
                return toDraftVo(detail, existing, false);
            }
        }
        String targetName = DisciplineAnnouncementDraftSupport.resolveTargetName(
                detail.getTargetType(), detail.getPlayerNo(), detail.getPlayerName(),
                detail.getEventTeamName(), detail.getTeamName(), detail.getEventTeamId(),
                detail.getTeamId(), detail.getPlayerId());
        String title = DisciplineAnnouncementDraftSupport.buildTitle(targetName, detail.getPenaltyCode());
        String publicStatement = req == null ? null : req.getPublicStatement();
        String content = DisciplineAnnouncementDraftSupport.buildContent(
                targetName, detail.getSeriesName(), detail.getSeasonName(),
                detail.getPenaltyCode(), detail.getEffectiveStartTime(), publicStatement);
        AnnouncementAddReq addReq = new AnnouncementAddReq();
        addReq.setCategoryCode("NOTICE");
        addReq.setTitle(title);
        addReq.setSummary(targetName + " " + DisciplineAnnouncementDraftSupport.penaltyLabel(detail.getPenaltyCode()));
        addReq.setContentText(content);
        addReq.setPublicScope("PUBLIC");
        addReq.setSeasonId(detail.getSeasonId());
        Long announcementId = announcementService.add(addReq);
        disciplineRecordMapper.updateAnnouncementId(id, announcementId);
        auditWriter.writeAudit("DISCIPLINE", "DISCIPLINE_RECORD", id, "LINK_ANNOUNCEMENT",
                "生成处罚公告草稿", null, null);
        BasketAnnouncement created = announcementMapper.selectById(announcementId);
        return toDraftVo(detail, created, true);
    }

    @Override
    public List<DisciplineSelectorOptionVo> selectSeasonOptions(String keyword) {
        return disciplineSelectorMapper.selectSeasonOptions(StringUtils.trim(keyword));
    }

    @Override
    public List<DisciplineSelectorOptionVo> selectPlayerOptions(Long seasonId, String keyword) {
        if (seasonId == null) {
            throw new ServiceException("请先选择所属赛季");
        }
        return disciplineSelectorMapper.selectPlayerOptions(seasonId, StringUtils.trim(keyword));
    }

    @Override
    public List<DisciplineSelectorOptionVo> selectTeamOptions(Long seasonId, String targetType, String keyword) {
        if (seasonId == null) {
            throw new ServiceException("请先选择所属赛季");
        }
        String type = StringUtils.isNotEmpty(targetType) ? targetType : DisciplineTargetTypeEnum.PERMANENT_TEAM.getCode();
        if (DisciplineTargetTypeEnum.EVENT_TEAM.getCode().equalsIgnoreCase(type)) {
            return disciplineSelectorMapper.selectEventTeamOptions(seasonId, StringUtils.trim(keyword));
        }
        if (DisciplineTargetTypeEnum.PERMANENT_TEAM.getCode().equalsIgnoreCase(type)) {
            return disciplineSelectorMapper.selectPermanentTeamOptions(seasonId, StringUtils.trim(keyword));
        }
        return Collections.emptyList();
    }

    private PenaltyParams normalizeAndValidatePenaltyParams(DisciplineCreateReq req, Date now) {
        DisciplinePenaltyCodeEnum code;
        try {
            code = DisciplinePenaltyCodeEnum.valueOf(req.getPenaltyCode());
        } catch (IllegalArgumentException ex) {
            throw new ServiceException("未知处罚措施编码");
        }
        int suspensionCount = req.getMatchSuspensionCount() != null ? req.getMatchSuspensionCount() : 0;
        int pointDeduction = req.getPointDeduction() != null ? req.getPointDeduction() : 0;
        Date start = req.getEffectiveStartTime();
        Date end = req.getEffectiveEndTime();
        switch (code) {
            case WARNING:
            case REGISTRATION_BAN:
            case TRANSFER_BAN:
            case CAPTAIN_BAN:
            case TEAM_SUSPENSION:
            case EVENT_DISQUALIFICATION:
            case PLAYER_QUALIFICATION_SUSPENSION:
                suspensionCount = 0;
                pointDeduction = 0;
                if (start == null) {
                    start = now;
                }
                break;
            case MATCH_SUSPENSION:
            case MATCH_ELIGIBILITY_CANCEL:
                if (suspensionCount < 1) {
                    throw new ServiceException("禁赛场次至少为 1");
                }
                pointDeduction = 0;
                if (start == null) {
                    start = now;
                }
                break;
            case POINT_DEDUCTION:
                if (pointDeduction < 1) {
                    throw new ServiceException("扣除积分至少为 1");
                }
                if (req.getEventTeamId() == null) {
                    throw new ServiceException(DisciplinePenaltyTargetCompatibilityPolicy.MSG_POINT_EVENT_TEAM_REQUIRED);
                }
                suspensionCount = 0;
                if (start == null) {
                    start = now;
                }
                break;
            case DATE_RANGE_SUSPENSION:
                suspensionCount = 0;
                pointDeduction = 0;
                if (start == null || end == null) {
                    throw new ServiceException("按日期禁赛必须填写生效时间与结束时间");
                }
                if (!start.before(end)) {
                    throw new ServiceException("生效时间必须早于结束时间");
                }
                break;
            default:
                throw new ServiceException("未知处罚措施编码");
        }
        return new PenaltyParams(suspensionCount, pointDeduction, start, end);
    }

    private void sanitizeCreateTargets(DisciplineCreateReq req) {
        String targetType = StringUtils.isNotEmpty(req.getTargetType())
                ? req.getTargetType() : DisciplineTargetTypeEnum.PLAYER.getCode();
        if (DisciplineTargetTypeEnum.PLAYER.getCode().equalsIgnoreCase(targetType)) {
            // 球员处罚主键为永久 playerId；teamId/eventTeamId 仅作赛事上下文
            return;
        }
        if (DisciplineTargetTypeEnum.PERMANENT_TEAM.getCode().equalsIgnoreCase(targetType)) {
            req.setPlayerId(null);
            req.setMemberId(null);
            req.setEventTeamId(null);
            return;
        }
        if (DisciplineTargetTypeEnum.EVENT_TEAM.getCode().equalsIgnoreCase(targetType)) {
            req.setPlayerId(null);
            req.setMemberId(null);
            return;
        }
        if (DisciplineTargetTypeEnum.ACCOUNT.getCode().equalsIgnoreCase(targetType)) {
            req.setPlayerId(null);
            req.setTeamId(null);
            req.setEventTeamId(null);
        }
    }

    private void validateCreateReq(DisciplineCreateReq req) {
        if (req == null) {
            throw new ServiceException("请求不能为空");
        }
        if (StringUtils.isEmpty(req.getTitle()) || StringUtils.isEmpty(req.getReasonText())) {
            throw new ServiceException("处罚标题与原因不能为空");
        }
        if (StringUtils.isEmpty(req.getDisciplineType()) || StringUtils.isEmpty(req.getPenaltyCode())) {
            throw new ServiceException("处罚类型与措施编码不能为空");
        }
        String targetType = StringUtils.isNotEmpty(req.getTargetType())
                ? req.getTargetType() : DisciplineTargetTypeEnum.PLAYER.getCode();
        DisciplinePenaltyTargetCompatibilityPolicy.validateCreate(req.getPenaltyCode(), targetType, req.getEventTeamId());
        if (DisciplineTargetTypeEnum.PLAYER.getCode().equalsIgnoreCase(targetType)) {
            if (req.getPlayerId() == null && req.getMemberId() == null) {
                throw new ServiceException("请选择处罚球员");
            }
            return;
        }
        if (DisciplineTargetTypeEnum.PERMANENT_TEAM.getCode().equalsIgnoreCase(targetType)) {
            if (req.getTeamId() == null) {
                throw new ServiceException("请选择处罚球队");
            }
            return;
        }
        if (DisciplineTargetTypeEnum.EVENT_TEAM.getCode().equalsIgnoreCase(targetType)) {
            if (req.getEventTeamId() == null) {
                throw new ServiceException("请选择处罚赛事球队");
            }
            return;
        }
        if (DisciplineTargetTypeEnum.ACCOUNT.getCode().equalsIgnoreCase(targetType)) {
            if (req.getMemberId() == null) {
                throw new ServiceException("请选择处罚账号");
            }
            return;
        }
        if (req.getPlayerId() == null && req.getMemberId() == null
                && req.getTeamId() == null && req.getEventTeamId() == null) {
            throw new ServiceException("处罚对象不能为空");
        }
    }

    private BasketDisciplineRecord requireRecord(Long id) {
        BasketDisciplineRecord record = disciplineRecordMapper.selectById(id);
        if (record == null) {
            throw new ServiceException("处罚记录不存在");
        }
        return record;
    }

    private void writePointAdjustmentIfNeeded(BasketDisciplineRecord record) {
        if (record.getPointDeduction() == null || record.getPointDeduction() <= 0 || record.getTeamId() == null) {
            return;
        }
        String idempotencyKey = "DISC:" + record.getId() + ":POINT";
        if (pointAdjustmentMapper.selectByIdempotencyKey(idempotencyKey) != null) {
            return;
        }
        Date now = new Date();
        BasketPointAdjustment adjustment = new BasketPointAdjustment();
        adjustment.setSeasonId(record.getSeasonId());
        adjustment.setTeamId(record.getTeamId());
        adjustment.setDisciplineId(record.getId());
        adjustment.setAdjustmentPoints(-record.getPointDeduction());
        adjustment.setAdjustmentReason(record.getTitle());
        adjustment.setIdempotencyKey(idempotencyKey);
        adjustment.setOperatorId(SecurityUtils.getUserId());
        adjustment.setCreateTime(now);
        adjustment.setUpdateTime(now);
        adjustment.setIsDelete(NOT_DELETED);
        pointAdjustmentMapper.insert(adjustment);
    }

    private void fillListDisplay(DisciplineListVo row, Date now) {
        if (row == null) {
            return;
        }
        if (DisciplineRecordStatusEnum.ACTIVE.name().equals(row.getRecordStatus())
                && row.getEffectiveEndTime() != null
                && now != null
                && row.getEffectiveEndTime().before(now)) {
            row.setBusinessStatus("EXPIRED");
        } else {
            row.setBusinessStatus(row.getRecordStatus());
        }
        row.setAnnouncementStatusName(DisciplineAnnouncementDraftSupport.resolveAnnouncementStatusName(row.getAnnouncementStatus()));
    }

    private void fillAnnouncementStatus(DisciplineDetailVo detail) {
        if (detail == null) {
            return;
        }
        detail.setAnnouncementStatusName(DisciplineAnnouncementDraftSupport.resolveAnnouncementStatusName(detail.getAnnouncementStatus()));
    }

    private DisciplineAnnouncementDraftVo toDraftVo(DisciplineDetailVo detail, BasketAnnouncement announcement, boolean created) {
        DisciplineAnnouncementDraftVo vo = new DisciplineAnnouncementDraftVo();
        vo.setDisciplineId(detail.getId());
        vo.setCreated(created);
        if (announcement != null) {
            vo.setAnnouncementId(announcement.getId());
            vo.setTitle(announcement.getTitle());
            vo.setContentText(announcement.getContentText());
            vo.setAnnouncementStatus(announcement.getAnnouncementStatus());
            vo.setAnnouncementStatusName(DisciplineAnnouncementDraftSupport.resolveAnnouncementStatusName(announcement.getAnnouncementStatus()));
        }
        return vo;
    }

    private void fillEventImpact(DisciplineDetailVo detail) {
        if (detail.getExecutionStatus() == null && detail.getEventTeamId() == null) {
            return;
        }
        PenaltyExecutionResultVo impact = new PenaltyExecutionResultVo();
        impact.setExecutionStatus(detail.getExecutionStatus());
        impact.setExecutedTime(detail.getExecutedTime());
        impact.setErrorMessage(detail.getExecutionError());
        impact.setEventTeamId(detail.getEventTeamId());
        impact.setEventTeamName(detail.getEventTeamName());
        impact.setSeasonId(detail.getSeasonId());
        impact.setSeasonName(detail.getSeasonName());
        impact.setFutureMatchForfeitCount(nz(detail.getExecutionMatchForfeitCount()));
        impact.setSeriesForfeitCount(nz(detail.getExecutionSeriesForfeitCount()));
        impact.setQualificationRemovedCount(nz(detail.getExecutionQualificationRemovedCount()));
        impact.setWarningCount(nz(detail.getExecutionWarningCount()));
        if (detail.getId() != null) {
            java.util.List<BasketDisciplineExecutionDetail> details = executionDetailMapper.selectByDisciplineId(detail.getId());
            impact.setDetails(details);
            detail.setExecutionDetails(details);
            if (details != null) {
                for (BasketDisciplineExecutionDetail row : details) {
                    if (row.getResultMessage() != null
                            && ("DOWNSTREAM_GUARD".equals(row.getActionType())
                            || "REQUIRES_ADMIN_ADJUDICATION".equals(row.getActionType())
                            || "QUALIFICATION_BLOCK".equals(row.getActionType()))) {
                        impact.getWarnings().add(row.getResultMessage());
                    }
                }
            }
        }
        detail.setEventImpact(impact);
    }

    private int nz(Integer value) {
        return value == null ? 0 : value;
    }

    private AuditFieldInput field(String name, String label, String before, String after) {
        AuditFieldInput input = new AuditFieldInput();
        input.setFieldName(name);
        input.setFieldLabel(label);
        input.setBeforeValue(before);
        input.setAfterValue(after);
        return input;
    }

    private static class PenaltyParams {
        private final int matchSuspensionCount;
        private final int pointDeduction;
        private final Date effectiveStartTime;
        private final Date effectiveEndTime;

        private PenaltyParams(int matchSuspensionCount, int pointDeduction, Date effectiveStartTime, Date effectiveEndTime) {
            this.matchSuspensionCount = matchSuspensionCount;
            this.pointDeduction = pointDeduction;
            this.effectiveStartTime = effectiveStartTime;
            this.effectiveEndTime = effectiveEndTime;
        }
    }
}
