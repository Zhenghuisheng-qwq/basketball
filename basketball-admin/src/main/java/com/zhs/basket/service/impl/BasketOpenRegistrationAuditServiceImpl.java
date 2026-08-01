package com.zhs.basket.service.impl;

import com.zhs.basket.domain.*;
import com.zhs.basket.enums.*;
import com.zhs.basket.mapper.*;
import com.zhs.basket.query.OpenRegistrationAuditQuery;
import com.zhs.basket.req.BatchAuditOperationReq;
import com.zhs.basket.req.OpenRegistrationAuditApproveReq;
import com.zhs.basket.req.OpenRegistrationAuditRejectReq;
import com.zhs.basket.support.BatchAuditSupport;
import com.zhs.basket.service.IBasketOpenRegistrationAuditService;
import com.zhs.basket.support.BasketAdminMessageService;
import com.zhs.basket.support.BasketDisciplineRestrictionSupport;
import com.zhs.basket.support.RosterActiveSlotSupport;
import com.zhs.basket.support.RosterNoGenerator;
import com.zhs.basket.vo.*;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import com.zhs.common.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BasketOpenRegistrationAuditServiceImpl implements IBasketOpenRegistrationAuditService {

    private static final Integer NOT_DELETED = 0;

    @Autowired private BasketTournamentOpenRegistrationMapper openRegistrationMapper;
    @Autowired private BasketTournamentOpenRegistrationAuditMapper openRegistrationAuditMapper;
    @Autowired private BasketTournamentSeasonRosterMapper seasonRosterMapper;
    @Autowired private BasketPlayerProfileMapper profileMapper;
    @Autowired private RosterNoGenerator rosterNoGenerator;
    @Autowired private BasketDisciplineRestrictionSupport disciplineRestrictionSupport;
    @Autowired private BasketAdminMessageService messageService;

    @Override
    public List<OpenRegistrationAuditListVo> selectList(OpenRegistrationAuditQuery query) {
        List<OpenRegistrationAuditListVo> list = openRegistrationMapper.selectAuditList(query);
        list.forEach(this::enrichListVo);
        return list;
    }

    @Override
    public OpenRegistrationAuditDetailVo selectDetail(Long id) {
        OpenRegistrationAuditDetailVo vo = openRegistrationMapper.selectDetailVo(id);
        if (vo == null) throw new ServiceException("Open报名记录不存在");
        vo.setRegistrationStatusName(OpenRegistrationStatusEnum.fromCode(vo.getRegistrationStatus()).getAuditDisplayDescription());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startReview(Long id) {
        BasketTournamentOpenRegistration reg = requireRegistration(id);
        int rows = openRegistrationMapper.updateStatusIfMatch(id,
                Collections.singletonList(OpenRegistrationStatusEnum.SUBMITTED.getCode()),
                OpenRegistrationStatusEnum.REVIEWING.getCode(), SecurityUtils.getUserId(), null, null,
                SecurityUtils.getUserId(), null);
        assertSingleRow(rows, "开始审核失败");
        writeAudit(reg, "START_REVIEW", reg.getRegistrationStatus(), OpenRegistrationStatusEnum.REVIEWING.getCode(), "开始审核");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, OpenRegistrationAuditApproveReq req) {
        BasketTournamentOpenRegistration reg = requireRegistration(id);
        if (seasonRosterMapper.countActiveBySeasonAndPlayer(reg.getSeasonId(), reg.getPlayerId()) > 0) {
            throw new ServiceException("该球员本赛季已有生效阵容");
        }
        disciplineRestrictionSupport.assertAllowRegistration("PLAYER", reg.getPlayerId(), reg.getSeasonId());
        if (reg.getMemberId() != null) {
            disciplineRestrictionSupport.assertAllowRegistration("ACCOUNT", reg.getMemberId(), reg.getSeasonId());
        }
        Date now = new Date();
        Long rosterId = createOpenRoster(reg, now);
        int rows = openRegistrationMapper.updateStatusIfMatch(id,
                Arrays.asList(OpenRegistrationStatusEnum.SUBMITTED.getCode(), OpenRegistrationStatusEnum.REVIEWING.getCode()),
                OpenRegistrationStatusEnum.APPROVED.getCode(), SecurityUtils.getUserId(),
                req != null ? req.getAuditRemark() : null, now, SecurityUtils.getUserId(), rosterId);
        assertSingleRow(rows, "审核通过失败");
        writeAudit(reg, "APPROVE", reg.getRegistrationStatus(), OpenRegistrationStatusEnum.APPROVED.getCode(),
                req != null ? req.getAuditRemark() : null);
        messageService.sendSiteMessage(reg.getMemberId(), SecurityUtils.getUserId(), "OPEN_REGISTRATION", id,
                "Open报名审核已通过", "您的Open报名已通过审核。");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, OpenRegistrationAuditRejectReq req) {
        BasketTournamentOpenRegistration reg = requireRegistration(id);
        String remark = req != null && StringUtils.hasText(req.getRejectReason()) ? req.getRejectReason()
                : (req != null ? req.getAuditRemark() : null);
        int rows = openRegistrationMapper.updateStatusIfMatch(id,
                Arrays.asList(OpenRegistrationStatusEnum.SUBMITTED.getCode(), OpenRegistrationStatusEnum.REVIEWING.getCode()),
                OpenRegistrationStatusEnum.REJECTED.getCode(), SecurityUtils.getUserId(), remark, new Date(),
                SecurityUtils.getUserId(), null);
        assertSingleRow(rows, "驳回失败");
        writeAudit(reg, "REJECT", reg.getRegistrationStatus(), OpenRegistrationStatusEnum.REJECTED.getCode(), remark);
        messageService.sendSiteMessage(reg.getMemberId(), SecurityUtils.getUserId(), "OPEN_REGISTRATION", id,
                "Open报名审核已驳回", remark);
    }

    @Override
    public BatchAuditResultVo batchApprove(BatchAuditOperationReq req) {
        OpenRegistrationAuditApproveReq approveReq = new OpenRegistrationAuditApproveReq();
        approveReq.setAuditRemark(req.getAuditRemark());
        return BatchAuditSupport.run(req.getIds(), id -> approve(id, approveReq));
    }

    @Override
    public BatchAuditResultVo batchReject(BatchAuditOperationReq req) {
        OpenRegistrationAuditRejectReq rejectReq = new OpenRegistrationAuditRejectReq();
        rejectReq.setRejectReason(req.getRejectReason());
        rejectReq.setAuditRemark(req.getAuditRemark());
        return BatchAuditSupport.run(req.getIds(), id -> reject(id, rejectReq));
    }

    @Override
    public List<OpenRegistrationAuditRecordVo> selectAuditRecords(Long id) {
        return openRegistrationAuditMapper.selectByRegistrationId(id).stream().map(a -> {
            OpenRegistrationAuditRecordVo vo = new OpenRegistrationAuditRecordVo();
            BeanUtils.copyBeanProp(vo, a);
            return vo;
        }).collect(Collectors.toList());
    }

    private Long createOpenRoster(BasketTournamentOpenRegistration reg, Date now) {
        BasketPlayerProfile profile = profileMapper.selectById(reg.getPlayerId());
        BasketTournamentSeasonRoster roster = new BasketTournamentSeasonRoster();
        roster.setRosterNo(rosterNoGenerator.generateRosterNo());
        roster.setSeasonId(reg.getSeasonId());
        roster.setPlayerId(reg.getPlayerId());
        roster.setMemberId(reg.getMemberId());
        roster.setOpenRegistrationId(reg.getId());
        roster.setPlayerNicknameSnapshot(profile != null ? profile.getGameNickname() : null);
        roster.setPlatformSnapshot(reg.getPlatform());
        roster.setGameIdDisplaySnapshot(reg.getDisplayGameId());
        roster.setGameIdNormalizedSnapshot(reg.getNormalizedGameId());
        roster.setPositionSnapshot(reg.getPositionCode());
        roster.setRosterRole("PLAYER");
        roster.setIsCaptain(0);
        roster.setIsStarting(0);
        roster.setIsSubstitute(1);
        roster.setSourceType(RosterSourceTypeEnum.OPEN_REGISTRATION.getCode());
        roster.setRosterStatus(SeasonRosterStatusEnum.ACTIVE.getCode());
        roster.setLockStatus(RosterLockStatusEnum.UNLOCKED.getCode());
        roster.setActiveSlot(RosterActiveSlotSupport.buildActiveSlot(reg.getSeasonId(), reg.getPlayerId()));
        roster.setEffectiveTime(now);
        roster.setCreateTime(now);
        roster.setUpdateTime(now);
        roster.setIsDelete(NOT_DELETED);
        seasonRosterMapper.insert(roster);
        return roster.getId();
    }

    private void writeAudit(BasketTournamentOpenRegistration reg, String action, Integer before, Integer after, String remark) {
        Date now = new Date();
        BasketTournamentOpenRegistrationAudit audit = new BasketTournamentOpenRegistrationAudit();
        audit.setRegistrationId(reg.getId());
        audit.setAuditAction(action);
        audit.setBeforeStatus(before);
        audit.setAfterStatus(after);
        audit.setAuditorId(SecurityUtils.getUserId());
        audit.setAuditorName(SecurityUtils.getUsername());
        audit.setAuditRemark(remark);
        audit.setAuditTime(now);
        audit.setCreateTime(now);
        audit.setUpdateTime(now);
        audit.setIsDelete(NOT_DELETED);
        openRegistrationAuditMapper.insert(audit);
    }

    private BasketTournamentOpenRegistration requireRegistration(Long id) {
        BasketTournamentOpenRegistration reg = openRegistrationMapper.selectById(id);
        if (reg == null) throw new ServiceException("Open报名记录不存在");
        return reg;
    }

    private void enrichListVo(OpenRegistrationAuditListVo vo) {
        vo.setRegistrationStatusName(OpenRegistrationStatusEnum.fromCode(vo.getRegistrationStatus()).getAuditDisplayDescription());
    }

    private void assertSingleRow(int rows, String message) {
        if (rows != 1) throw new ServiceException(message);
    }
}
