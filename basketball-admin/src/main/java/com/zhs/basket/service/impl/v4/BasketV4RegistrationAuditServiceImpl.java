package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.BasketTeam;
import com.zhs.basket.domain.v4.BasketCaptainGrant;
import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketRegistration;
import com.zhs.basket.domain.v4.BasketRegistrationAudit;
import com.zhs.basket.domain.v4.BasketRegistrationPlayer;
import com.zhs.basket.dto.v4.BasketRegistrationAuditRejectReq;
import com.zhs.basket.enums.v4.AuditResultEnum;
import com.zhs.basket.enums.v4.ParticipationRoleEnum;
import com.zhs.basket.enums.v4.RegistrationStatusEnum;
import com.zhs.basket.support.v4.roster.TeamRosterSupport;
import com.zhs.basket.mapper.BasketTeamMapper;
import com.zhs.basket.mapper.v4.BasketCaptainGrantMapper;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationAuditMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationPlayerMapper;
import com.zhs.basket.query.v4.BasketRegistrationAuditQuery;
import com.zhs.basket.service.v4.IBasketV4EventSnapshotService;
import com.zhs.basket.service.v4.IBasketV4RegistrationAuditService;
import com.zhs.basket.support.BasketAdminMessageService;
import com.zhs.basket.support.NoticeParams;
import com.zhs.basket.support.v4.CaptainGrantRuntimeSupport;
import com.zhs.basket.support.v4.CurrentRosterEligibility;
import com.zhs.basket.support.v4.TeamRegistrationEligibilitySpecification;
import com.zhs.basket.vo.v4.BasketRegistrationAuditDetailVo;
import com.zhs.basket.vo.v4.BasketRegistrationAuditListVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class BasketV4RegistrationAuditServiceImpl implements IBasketV4RegistrationAuditService {

    private static final Integer NOT_DELETED = 0;

    @Autowired
    private BasketRegistrationMapper registrationMapper;
    @Autowired
    private BasketRegistrationPlayerMapper registrationPlayerMapper;
    @Autowired
    private BasketRegistrationAuditMapper registrationAuditMapper;
    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private BasketCaptainGrantMapper captainGrantMapper;
    @Autowired
    private IBasketV4EventSnapshotService eventSnapshotService;
    @Autowired
    private TeamRosterSupport teamRosterSupport;
    @Autowired
    private TeamRegistrationEligibilitySpecification teamRegistrationEligibilitySpecification;
    @Autowired
    private CaptainGrantRuntimeSupport grantRuntimeSupport;
    @Autowired
    private BasketAdminMessageService messageService;
    @Autowired
    private BasketTeamMapper teamMapper;

    @Override
    public List<BasketRegistrationAuditListVo> selectList(BasketRegistrationAuditQuery query) {
        List<BasketRegistrationAuditListVo> rows = registrationMapper.selectAuditVoList(query);
        for (BasketRegistrationAuditListVo row : rows) {
            int current = row.getCurrentEligibleCount() == null ? 0 : row.getCurrentEligibleCount();
            int min = row.getLineupMinCount() == null ? 0 : row.getLineupMinCount();
            row.setEligibilityStatus(CurrentRosterEligibility.status(row.getRegistrationStatus(), current, min));
        }
        return rows;
    }

    @Override
    public BasketRegistrationAuditDetailVo selectDetail(Long registrationId) {
        BasketRegistration registration = requireRegistration(registrationId);
        BasketRegistrationAuditDetailVo vo = new BasketRegistrationAuditDetailVo();
        vo.setRegistrationId(registration.getId());
        vo.setSeasonId(registration.getSeasonId());
        vo.setDivisionId(registration.getDivisionId());
        vo.setRegistrationMode(registration.getRegistrationMode());
        vo.setTeamId(registration.getTeamId());
        vo.setCaptainPlayerId(registration.getCaptainPlayerId());
        vo.setCaptainMemberId(registration.getCaptainMemberId());
        vo.setLineupCount(registration.getLineupCount());
        vo.setRegistrationStatus(registration.getRegistrationStatus());
        vo.setSubmitTime(registration.getSubmitTime());
        vo.setApproveTime(registration.getApproveTime());
        vo.setRejectReason(registration.getRejectReason());
        vo.setPlayers(registrationPlayerMapper.selectVoByRegistrationId(registrationId));
        vo.setAudits(registrationAuditMapper.selectVoByRegistrationId(registrationId));

        BasketRegistrationAuditQuery query = new BasketRegistrationAuditQuery();
        query.setSeasonId(registration.getSeasonId());
        List<BasketRegistrationAuditListVo> baseRows = registrationMapper.selectAuditVoList(query);
        baseRows.stream()
                .filter(row -> registrationId.equals(row.getRegistrationId()))
                .findFirst()
                .ifPresent(row -> {
                    vo.setSeasonTitle(row.getSeasonTitle());
                    vo.setDivisionName(row.getDivisionName());
                    vo.setPlatformCode(row.getPlatformCode());
                    vo.setTeamName(row.getTeamName());
                    vo.setCaptainPlayerName(row.getCaptainPlayerName());
                });
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long registrationId) {
        BasketRegistration registration = requireRegistration(registrationId);
        if (!RegistrationStatusEnum.PENDING_REVIEW.getCode().equals(registration.getRegistrationStatus())) {
            throw new ServiceException("仅待审核状态可通过");
        }
        divisionMapper.selectByIdForUpdate(registration.getDivisionId());
        BasketDivision division = requireDivision(registration.getDivisionId());
        List<BasketRegistrationPlayer> players = registrationPlayerMapper.selectByRegistrationId(registrationId);
        BasketCaptainGrant grant = captainGrantMapper.selectActiveByDivisionPlayer(
                registration.getDivisionId(), registration.getCaptainPlayerId());
        grantRuntimeSupport.lazyExpireIfNeeded(grant, null);
        grant = captainGrantMapper.selectActiveByDivisionPlayer(
                registration.getDivisionId(), registration.getCaptainPlayerId());
        teamRegistrationEligibilitySpecification.validateRegistration(division, registration, grant, players);

        Date now = new Date();
        String beforeStatus = registration.getRegistrationStatus();
        registration.setRegistrationStatus(RegistrationStatusEnum.APPROVED.getCode());
        registration.setApproveTime(now);
        registration.setRejectReason(null);
        registration.setAuditUserId(SecurityUtils.getUserId());
        registration.setUpdateTime(now);
        if (teamRosterSupport.isTeamDirect(registration)) {
            registration.setParticipationRole(ParticipationRoleEnum.UNALLOCATED.getCode());
            registration.setCandidateOrder(null);
        }
        if (registrationMapper.updateById(registration) != 1) {
            throw new ServiceException("审核通过失败");
        }

        writeAudit(registration, "APPROVE", beforeStatus, RegistrationStatusEnum.APPROVED.getCode(),
                AuditResultEnum.APPROVED.getCode(), null);
        if (!teamRosterSupport.isTeamDirect(registration)) {
            eventSnapshotService.generateFromApprovedRegistration(registration);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long registrationId, BasketRegistrationAuditRejectReq req) {
        if (req == null || StringUtils.isEmpty(req.getRejectReason())) {
            throw new ServiceException("驳回原因不能为空");
        }
        BasketRegistration registration = requireRegistration(registrationId);
        if (!RegistrationStatusEnum.PENDING_REVIEW.getCode().equals(registration.getRegistrationStatus())) {
            throw new ServiceException("仅待审核状态可驳回");
        }

        Date now = new Date();
        String beforeStatus = registration.getRegistrationStatus();
        registration.setRegistrationStatus(RegistrationStatusEnum.REJECTED.getCode());
        registration.setRejectReason(req.getRejectReason().trim());
        registration.setAuditUserId(SecurityUtils.getUserId());
        registration.setAuditRemark(req.getAuditRemark());
        registration.setUpdateTime(now);
        if (registrationMapper.updateById(registration) != 1) {
            throw new ServiceException("审核驳回失败");
        }

        writeAudit(registration, "REJECT", beforeStatus, RegistrationStatusEnum.REJECTED.getCode(),
                AuditResultEnum.REJECTED.getCode(), req.getRejectReason());
        notifyTeamRegistrationRejected(registration, req.getRejectReason().trim());
    }

    private void notifyTeamRegistrationRejected(BasketRegistration registration, String rejectReason) {
        if (registration.getCaptainMemberId() == null) {
            return;
        }
        String teamName = null;
        if (registration.getTeamId() != null) {
            BasketTeam team = teamMapper.selectById(registration.getTeamId());
            if (team != null) {
                teamName = team.getTeamName();
            }
        }
        BasketDivision division = divisionMapper.selectById(registration.getDivisionId());
        String divisionLabel = division != null && StringUtils.isNotEmpty(division.getDivisionName())
                ? division.getDivisionName() : "当前赛区";
        String subject = StringUtils.isNotEmpty(teamName) ? ("球队「" + teamName + "」") : "您的球队";
        messageService.sendSiteMessage(
                registration.getCaptainMemberId(),
                SecurityUtils.getUserId(),
                "TEAM_REGISTRATION",
                registration.getId(),
                "球队报名已驳回",
                subject + "在「" + divisionLabel + "」的报名未通过审核，原因：" + rejectReason,
                "TEAM_REGISTRATION_REJECTED",
                NoticeParams.json("teamLabel", subject, "divisionName", divisionLabel, "remark", rejectReason));
    }

    private void writeAudit(BasketRegistration registration, String action, String beforeStatus, String afterStatus,
                            String auditResult, String remark) {
        Date now = new Date();
        BasketRegistrationAudit audit = new BasketRegistrationAudit();
        audit.setRegistrationId(registration.getId());
        audit.setDivisionId(registration.getDivisionId());
        audit.setSeasonId(registration.getSeasonId());
        audit.setAuditAction(action);
        audit.setAuditResult(auditResult);
        audit.setBeforeStatus(beforeStatus);
        audit.setAfterStatus(afterStatus);
        audit.setAuditUserId(SecurityUtils.getUserId());
        audit.setAuditUserName(SecurityUtils.getUsername());
        audit.setAuditTime(now);
        audit.setAuditRemark(remark);
        audit.setCreateTime(now);
        audit.setUpdateTime(now);
        audit.setIsDelete(NOT_DELETED);
        registrationAuditMapper.insert(audit);
    }

    private BasketRegistration requireRegistration(Long id) {
        if (id == null) {
            throw new ServiceException("registrationId 不能为空");
        }
        BasketRegistration registration = registrationMapper.selectById(id);
        if (registration == null) {
            throw new ServiceException("报名不存在");
        }
        return registration;
    }

    private BasketDivision requireDivision(Long divisionId) {
        BasketDivision division = divisionMapper.selectById(divisionId);
        if (division == null) {
            throw new ServiceException("Division 不存在");
        }
        return division;
    }
}
