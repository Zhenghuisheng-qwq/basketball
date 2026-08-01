package com.zhs.basket.service.impl;

import com.zhs.basket.domain.*;
import com.zhs.basket.enums.*;
import com.zhs.basket.mapper.*;
import com.zhs.basket.query.RosterChangeAuditQuery;
import com.zhs.basket.req.BatchAuditOperationReq;
import com.zhs.basket.req.RosterChangeAuditApproveReq;
import com.zhs.basket.req.RosterChangeAuditRejectReq;
import com.zhs.basket.support.BatchAuditSupport;
import com.zhs.basket.vo.BatchAuditResultVo;
import com.zhs.basket.service.IBasketRosterChangeAuditService;
import com.zhs.basket.support.BasketAdminMessageService;
import com.zhs.basket.support.BasketDisciplineRestrictionSupport;
import com.zhs.basket.support.RosterActiveSlotSupport;
import com.zhs.basket.support.RosterNoGenerator;
import com.zhs.basket.vo.RosterChangeAuditDetailVo;
import com.zhs.basket.vo.RosterChangeAuditListVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BasketRosterChangeAuditServiceImpl implements IBasketRosterChangeAuditService {

    private static final Integer NOT_DELETED = 0;

    @Autowired private BasketTournamentRosterChangeRequestMapper changeRequestMapper;
    @Autowired private BasketTournamentSeasonRosterMapper seasonRosterMapper;
    @Autowired private BasketTournamentRosterChangeRecordMapper changeRecordMapper;
    @Autowired private BasketTeamSeasonProfileMapper teamSeasonProfileMapper;
    @Autowired private BasketPlayerProfileMapper profileMapper;
    @Autowired private RosterNoGenerator rosterNoGenerator;
    @Autowired private BasketDisciplineRestrictionSupport disciplineRestrictionSupport;
    @Autowired private BasketAdminMessageService messageService;

    @Override
    public List<RosterChangeAuditListVo> selectList(RosterChangeAuditQuery query) {
        List<RosterChangeAuditListVo> list = changeRequestMapper.selectAuditList(query);
        list.forEach(this::enrichListVo);
        return list;
    }

    @Override
    public RosterChangeAuditDetailVo selectDetail(Long id) {
        RosterChangeAuditDetailVo vo = changeRequestMapper.selectDetailVo(id);
        if (vo == null) throw new ServiceException("阵容变更申请不存在");
        enrichDetailVo(vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, RosterChangeAuditApproveReq req) {
        BasketTournamentRosterChangeRequest request = requireRequest(id);
        Date now = new Date();
        Long resultRosterId = applyChange(request, now);
        int rows = changeRequestMapper.updateStatusIfMatch(id,
                Collections.singletonList(RosterChangeStatusEnum.PENDING.getCode()),
                RosterChangeStatusEnum.APPROVED.getCode(), SecurityUtils.getUserId(),
                req != null ? req.getAuditRemark() : null, null, now, resultRosterId);
        assertSingleRow(rows, "审核通过失败");
        messageService.sendSiteMessage(request.getApplicantMemberId(), SecurityUtils.getUserId(), "ROSTER_CHANGE", id,
                "阵容变更已通过", "您的阵容变更申请已通过审核。");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, RosterChangeAuditRejectReq req) {
        BasketTournamentRosterChangeRequest request = requireRequest(id);
        String remark = req != null && StringUtils.hasText(req.getRejectReason()) ? req.getRejectReason()
                : (req != null ? req.getAuditRemark() : null);
        int rows = changeRequestMapper.updateStatusIfMatch(id,
                Collections.singletonList(RosterChangeStatusEnum.PENDING.getCode()),
                RosterChangeStatusEnum.REJECTED.getCode(), SecurityUtils.getUserId(), null, remark, new Date(), null);
        assertSingleRow(rows, "驳回失败");
        messageService.sendSiteMessage(request.getApplicantMemberId(), SecurityUtils.getUserId(), "ROSTER_CHANGE", id,
                "阵容变更已驳回", remark);
    }

    @Override
    public BatchAuditResultVo batchApprove(BatchAuditOperationReq req) {
        RosterChangeAuditApproveReq approveReq = new RosterChangeAuditApproveReq();
        approveReq.setAuditRemark(req.getAuditRemark());
        return BatchAuditSupport.run(req.getIds(), id -> approve(id, approveReq));
    }

    @Override
    public BatchAuditResultVo batchReject(BatchAuditOperationReq req) {
        RosterChangeAuditRejectReq rejectReq = new RosterChangeAuditRejectReq();
        rejectReq.setRejectReason(req.getRejectReason());
        rejectReq.setAuditRemark(req.getAuditRemark());
        return BatchAuditSupport.run(req.getIds(), id -> reject(id, rejectReq));
    }

    private Long applyChange(BasketTournamentRosterChangeRequest request, Date now) {
        RosterChangeTypeEnum changeType = RosterChangeTypeEnum.fromCode(request.getChangeType());
        assertDisciplineAllowed(request, changeType);
        return switch (changeType) {
            case TRANSFER -> applyTransfer(request, now);
            case FREE_AGENT_ADD -> applyFreeAgentAdd(request, now);
            case CAPTAIN_TRANSFER -> applyCaptainTransfer(request, now);
        };
    }

    private Long applyTransfer(BasketTournamentRosterChangeRequest request, Date now) {
        BasketTournamentSeasonRoster source = requireActiveRoster(request.getSourceRosterId());
        assertRosterUnlocked(source.getEventTeamId());
        seasonRosterMapper.invalidateActiveRoster(source.getId(), SeasonRosterStatusEnum.TRANSFERRED_OUT.getCode(), now);
        writeChangeRecord(request, source, null, "转出", now);
        return insertTargetRoster(request, source, now, RosterSourceTypeEnum.TRANSFER);
    }

    private Long applyFreeAgentAdd(BasketTournamentRosterChangeRequest request, Date now) {
        if (seasonRosterMapper.countActiveBySeasonAndPlayer(request.getSeasonId(), request.getPlayerId()) > 0) {
            throw new ServiceException("该球员本赛季已有生效阵容");
        }
        assertRosterUnlocked(request.getTargetEventTeamId());
        BasketTournamentSeasonRoster pseudo = new BasketTournamentSeasonRoster();
        pseudo.setSeasonId(request.getSeasonId());
        pseudo.setPlayerId(request.getPlayerId());
        pseudo.setMemberId(request.getApplicantMemberId());
        return insertTargetRoster(request, pseudo, now, RosterSourceTypeEnum.FREE_AGENT_ADD);
    }

    private Long applyCaptainTransfer(BasketTournamentRosterChangeRequest request, Date now) {
        assertRosterUnlocked(request.getTargetEventTeamId());
        seasonRosterMapper.clearCaptainByEventTeam(request.getTargetEventTeamId());
        seasonRosterMapper.updateCaptainFlag(request.getTargetEventTeamId(), request.getNewCaptainPlayerId(), 1);
        teamSeasonProfileMapper.updateCaptainPlayerId(request.getTargetEventTeamId(), request.getNewCaptainPlayerId());
        writeChangeRecord(request, null, null, "队长变更", now);
        return request.getSourceRosterId();
    }

    private Long insertTargetRoster(BasketTournamentRosterChangeRequest request, BasketTournamentSeasonRoster source,
            Date now, RosterSourceTypeEnum sourceType) {
        BasketPlayerProfile profile = profileMapper.selectById(request.getPlayerId());
        BasketTournamentSeasonRoster roster = new BasketTournamentSeasonRoster();
        roster.setRosterNo(rosterNoGenerator.generateRosterNo());
        roster.setSeasonId(request.getSeasonId());
        roster.setEventTeamId(request.getTargetEventTeamId());
        roster.setTeamId(source.getTeamId());
        roster.setPlayerId(request.getPlayerId());
        roster.setMemberId(source.getMemberId() != null ? source.getMemberId() : request.getApplicantMemberId());
        roster.setPlayerNicknameSnapshot(profile != null ? profile.getGameNickname() : null);
        roster.setPlatformSnapshot(source.getPlatformSnapshot());
        roster.setGameIdDisplaySnapshot(source.getGameIdDisplaySnapshot());
        roster.setGameIdNormalizedSnapshot(source.getGameIdNormalizedSnapshot());
        roster.setPositionSnapshot(profile != null ? profile.getPositionCode() : null);
        roster.setRosterRole(StringUtils.hasText(request.getTargetRosterRole()) ? request.getTargetRosterRole() : "PLAYER");
        roster.setIsCaptain(0);
        roster.setIsStarting(request.getTargetIsStarting() != null ? request.getTargetIsStarting() : 0);
        roster.setIsSubstitute(request.getTargetIsSubstitute() != null ? request.getTargetIsSubstitute() : 1);
        roster.setSourceType(sourceType.getCode());
        roster.setRosterStatus(SeasonRosterStatusEnum.ACTIVE.getCode());
        roster.setLockStatus(RosterLockStatusEnum.UNLOCKED.getCode());
        roster.setActiveSlot(RosterActiveSlotSupport.buildActiveSlot(request.getSeasonId(), request.getPlayerId()));
        roster.setEffectiveTime(now);
        roster.setCreateTime(now);
        roster.setUpdateTime(now);
        roster.setIsDelete(NOT_DELETED);
        seasonRosterMapper.insert(roster);
        writeChangeRecord(request, source, roster, "变更生效", now);
        return roster.getId();
    }

    private void writeChangeRecord(BasketTournamentRosterChangeRequest request, BasketTournamentSeasonRoster source,
            BasketTournamentSeasonRoster target, String action, Date now) {
        BasketTournamentRosterChangeRecord record = new BasketTournamentRosterChangeRecord();
        record.setSeasonId(request.getSeasonId());
        record.setChangeRequestId(request.getId());
        record.setChangeType(request.getChangeType());
        record.setPlayerId(request.getPlayerId());
        record.setSourceEventTeamId(request.getSourceEventTeamId());
        record.setTargetEventTeamId(request.getTargetEventTeamId());
        record.setSourceRosterId(source != null ? source.getId() : request.getSourceRosterId());
        record.setTargetRosterId(target != null ? target.getId() : null);
        record.setBeforeSnapshot(source != null ? source.getRosterRole() : null);
        record.setAfterSnapshot(target != null ? target.getRosterRole() : action);
        record.setOperatorId(SecurityUtils.getUserId());
        record.setOperatorName(SecurityUtils.getUsername());
        record.setOperateReason(action);
        record.setEffectiveTime(now);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        record.setIsDelete(NOT_DELETED);
        changeRecordMapper.insert(record);
    }

    private void assertDisciplineAllowed(BasketTournamentRosterChangeRequest request, RosterChangeTypeEnum changeType) {
        Long seasonId = request.getSeasonId();
        switch (changeType) {
            case TRANSFER, FREE_AGENT_ADD -> {
                if (request.getPlayerId() != null) {
                    disciplineRestrictionSupport.assertAllowTransfer("PLAYER", request.getPlayerId(), seasonId);
                }
                if (request.getApplicantMemberId() != null) {
                    disciplineRestrictionSupport.assertAllowTransfer("ACCOUNT", request.getApplicantMemberId(), seasonId);
                }
            }
            case CAPTAIN_TRANSFER -> {
                if (request.getNewCaptainPlayerId() != null) {
                    disciplineRestrictionSupport.assertAllowCaptain("PLAYER", request.getNewCaptainPlayerId(), seasonId);
                }
            }
        }
    }

    private void assertRosterUnlocked(Long eventTeamId) {
        if (eventTeamId == null) return;
        BasketTeamSeasonProfile profile = teamSeasonProfileMapper.selectById(eventTeamId);
        if (profile != null && Objects.equals(profile.getRosterLockStatus(), RosterLockStatusEnum.LOCKED.getCode())) {
            throw new ServiceException("赛事球队阵容已锁定");
        }
    }

    private BasketTournamentSeasonRoster requireActiveRoster(Long rosterId) {
        BasketTournamentSeasonRoster roster = seasonRosterMapper.selectById(rosterId);
        if (roster == null || !Objects.equals(roster.getRosterStatus(), SeasonRosterStatusEnum.ACTIVE.getCode())) {
            throw new ServiceException("原阵容不存在或未生效");
        }
        return roster;
    }

    private BasketTournamentRosterChangeRequest requireRequest(Long id) {
        BasketTournamentRosterChangeRequest request = changeRequestMapper.selectById(id);
        if (request == null) throw new ServiceException("阵容变更申请不存在");
        return request;
    }

    private void enrichListVo(RosterChangeAuditListVo vo) {
        vo.setChangeTypeName(RosterChangeTypeEnum.fromCode(vo.getChangeType()).getDescription());
        vo.setRequestStatusName(RosterChangeStatusEnum.fromCode(vo.getRequestStatus()).getDescription());
    }

    private void enrichDetailVo(RosterChangeAuditDetailVo vo) {
        vo.setChangeTypeName(RosterChangeTypeEnum.fromCode(vo.getChangeType()).getDescription());
        vo.setRequestStatusName(RosterChangeStatusEnum.fromCode(vo.getRequestStatus()).getDescription());
    }

    private void assertSingleRow(int rows, String message) {
        if (rows != 1) throw new ServiceException(message);
    }
}
