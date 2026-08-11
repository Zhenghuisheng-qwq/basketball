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
import com.zhs.basket.service.IBasketDisciplineService;
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
    @Autowired private BasketDisciplineRestrictionMapper restrictionMapper;
    @Autowired private BasketPointAdjustmentMapper pointAdjustmentMapper;
    @Autowired private BasketDisciplineSuspensionLogMapper suspensionLogMapper;
    @Autowired private BasketMatchMapper matchMapper;
    @Autowired private com.zhs.basket.mapper.v4.BasketSeasonMapper seasonMapper;
    @Autowired private BasketBusinessAuditWriter auditWriter;

    @Override
    public List<DisciplineListVo> list(DisciplineQuery query) {
        return disciplineRecordMapper.selectList(query != null ? query : new DisciplineQuery());
    }

    @Override
    public DisciplineDetailVo detail(Long id) {
        DisciplineDetailVo detail = disciplineRecordMapper.selectDetailById(id);
        if (detail == null) {
            throw new ServiceException("处罚记录不存在");
        }
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
    @Transactional(rollbackFor = Exception.class)
    public void activate(Long id) {
        BasketDisciplineRecord record = requireRecord(id);
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
        upsertRestriction(record);
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
        RestrictionSubject subject = resolveSubject(record);
        if (subject != null) {
            restrictionMapper.deactivateBySubject(subject.subjectType, subject.subjectId, record.getSeasonId());
        }
        auditWriter.writeAudit("DISCIPLINE", "DISCIPLINE_RECORD", id, "REVOKE", "撤销处罚", req.getRevokeReason(), null);
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
        RestrictionSubject subject = resolveSubject(record);
        if (subject != null) {
            BasketDisciplineRestriction restriction = restrictionMapper.selectActiveBySubject(
                    subject.subjectType, subject.subjectId, record.getSeasonId());
            if (restriction != null) {
                restrictionMapper.updateRemainingSuspensions(restriction.getId(), nextRemaining);
                if (nextRemaining <= 0 && restriction.getAllowLineup() != null && restriction.getAllowLineup() == 0) {
                    restrictionMapper.deactivateBySubject(subject.subjectType, subject.subjectId, record.getSeasonId());
                }
            }
        }
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
                if (req.getTeamId() == null) {
                    throw new ServiceException("扣分处罚需要关联球队");
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

    private void upsertRestriction(BasketDisciplineRecord record) {
        RestrictionSubject subject = resolveSubject(record);
        if (subject == null) {
            return;
        }
        restrictionMapper.deactivateBySubject(subject.subjectType, subject.subjectId, record.getSeasonId());
        BasketDisciplineRestriction restriction = buildRestriction(record, subject);
        Date now = new Date();
        restriction.setCreateTime(now);
        restriction.setUpdateTime(now);
        restriction.setIsDelete(NOT_DELETED);
        restrictionMapper.insert(restriction);
    }

    private BasketDisciplineRestriction buildRestriction(BasketDisciplineRecord record, RestrictionSubject subject) {
        BasketDisciplineRestriction row = new BasketDisciplineRestriction();
        row.setSubjectType(subject.subjectType);
        row.setSubjectId(subject.subjectId);
        row.setSeasonId(record.getSeasonId());
        row.setAllowRegistration(1);
        row.setAllowTransfer(1);
        row.setAllowCaptain(1);
        row.setAllowLineup(1);
        row.setAllowParticipation(1);
        row.setRemainingMatchSuspensions(0);
        row.setEffectiveDisciplineId(record.getId());
        row.setActiveFlag(1);
        row.setEffectiveStartTime(record.getEffectiveStartTime());
        row.setEffectiveEndTime(record.getEffectiveEndTime());
        applyPenaltyCode(row, record);
        return row;
    }

    private void applyPenaltyCode(BasketDisciplineRestriction row, BasketDisciplineRecord record) {
        String penaltyCode = record.getPenaltyCode();
        if (StringUtils.isEmpty(penaltyCode)) {
            return;
        }
        try {
            DisciplinePenaltyCodeEnum code = DisciplinePenaltyCodeEnum.valueOf(penaltyCode);
            switch (code) {
                case REGISTRATION_BAN:
                    row.setAllowRegistration(0);
                    break;
                case TRANSFER_BAN:
                    row.setAllowTransfer(0);
                    break;
                case CAPTAIN_BAN:
                    row.setAllowCaptain(0);
                    break;
                case MATCH_SUSPENSION:
                case MATCH_ELIGIBILITY_CANCEL:
                    row.setAllowLineup(0);
                    row.setRemainingMatchSuspensions(safeInt(record.getMatchSuspensionRemaining()));
                    break;
                case DATE_RANGE_SUSPENSION:
                case EVENT_DISQUALIFICATION:
                case PLAYER_QUALIFICATION_SUSPENSION:
                    row.setAllowParticipation(0);
                    row.setAllowLineup(0);
                    break;
                case TEAM_SUSPENSION:
                    row.setAllowRegistration(0);
                    row.setAllowParticipation(0);
                    break;
                case POINT_DEDUCTION:
                case WARNING:
                default:
                    break;
            }
        } catch (IllegalArgumentException ignored) {
            // unknown penalty code ignored
        }
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

    private RestrictionSubject resolveSubject(BasketDisciplineRecord record) {
        String targetType = StringUtils.isNotEmpty(record.getTargetType())
                ? record.getTargetType() : DisciplineTargetTypeEnum.PLAYER.getCode();
        if (DisciplineTargetTypeEnum.PLAYER.getCode().equalsIgnoreCase(targetType) && record.getPlayerId() != null) {
            return new RestrictionSubject(DisciplineTargetTypeEnum.PLAYER.getCode(), record.getPlayerId());
        }
        if (DisciplineTargetTypeEnum.PERMANENT_TEAM.getCode().equalsIgnoreCase(targetType) && record.getTeamId() != null) {
            return new RestrictionSubject(DisciplineTargetTypeEnum.PERMANENT_TEAM.getCode(), record.getTeamId());
        }
        if (DisciplineTargetTypeEnum.EVENT_TEAM.getCode().equalsIgnoreCase(targetType) && record.getEventTeamId() != null) {
            return new RestrictionSubject(DisciplineTargetTypeEnum.EVENT_TEAM.getCode(), record.getEventTeamId());
        }
        if (record.getMemberId() != null) {
            return new RestrictionSubject(DisciplineTargetTypeEnum.ACCOUNT.getCode(), record.getMemberId());
        }
        if (record.getPlayerId() != null) {
            return new RestrictionSubject(DisciplineTargetTypeEnum.PLAYER.getCode(), record.getPlayerId());
        }
        if (record.getTeamId() != null) {
            return new RestrictionSubject(DisciplineTargetTypeEnum.PERMANENT_TEAM.getCode(), record.getTeamId());
        }
        return null;
    }

    private AuditFieldInput field(String name, String label, String before, String after) {
        AuditFieldInput input = new AuditFieldInput();
        input.setFieldName(name);
        input.setFieldLabel(label);
        input.setBeforeValue(before);
        input.setAfterValue(after);
        return input;
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private static class RestrictionSubject {
        private final String subjectType;
        private final Long subjectId;

        private RestrictionSubject(String subjectType, Long subjectId) {
            this.subjectType = subjectType;
            this.subjectId = subjectId;
        }
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
