package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.dao.BasketCaptainQualificationDao;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.BasketTeamDao;
import com.zhs.dao.BasketTeamMemberDao;
import com.zhs.dao.BasketTournamentRegistrationDao;
import com.zhs.enums.CaptainQualificationStatusEnum;
import com.zhs.enums.PlayerCertStatusEnum;
import com.zhs.enums.TeamMembershipStatusEnum;
import com.zhs.enums.TeamStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketCaptainQualification;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamMember;
import com.zhs.model.BasketTournamentRegistration;
import com.zhs.response.team.CaptainQualificationDetailResp;
import com.zhs.service.ICaptainPermissionService;
import com.zhs.support.CaptainQualificationValiditySupport;
import com.zhs.support.PlatformTeamMembershipSupport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;

@Service
public class CaptainPermissionServiceImpl implements ICaptainPermissionService {

    private static final Integer NOT_DELETED = 0;
    private static final int IS_CURRENT = 1;

    @Resource
    private BasketCaptainQualificationDao captainQualificationDao;
    @Resource
    private BasketPlayerProfileDao playerProfileDao;
    @Resource
    private BasketTeamDao teamDao;
    @Resource
    private BasketTeamMemberDao teamMemberDao;
    @Resource
    private BasketTournamentRegistrationDao registrationDao;
    @Resource
    private PlatformTeamMembershipSupport platformTeamMembershipSupport;

    @Override
    public CaptainQualificationDetailResp getCurrentQualification(Long accountId) {
        CaptainQualificationDetailResp resp = new CaptainQualificationDetailResp();
        BasketCaptainQualification qualification = loadCurrentQualification(accountId);
        if (qualification == null) {
            resp.setHasQualification(false);
            resp.setCanCreateTeam(false);
            resp.setRuntimeValid(false);
            resp.setInvalidReason("暂无有效队长资格");
            return resp;
        }

        CaptainQualificationStatusEnum status = CaptainQualificationStatusEnum.fromCode(qualification.getQualificationStatus());
        resp.setHasQualification(true);
        resp.setQualificationNo(qualification.getQualificationNo());
        resp.setStatus(qualification.getQualificationStatus());
        resp.setStatusText(status.getDescription());
        resp.setPaymentConfirmed(qualification.getPaymentConfirmed());
        resp.setValidFrom(qualification.getValidFrom());
        resp.setValidTo(qualification.getValidTo());
        resp.setIsCurrent(qualification.getIsCurrent());

        String invalidReason = resolveInvalidReason(accountId, qualification);
        resp.setInvalidReason(invalidReason);
        resp.setCanCreateTeam(invalidReason == null);
        resp.setRuntimeValid(invalidReason == null);
        return resp;
    }

    @Override
    public BasketCaptainQualification requireValidQualification(Long accountId) {
        BasketCaptainQualification qualification = loadCurrentQualification(accountId);
        String invalidReason = resolveInvalidReason(accountId, qualification);
        if (invalidReason != null) {
            throw new ServiceException(invalidReason, 403);
        }
        if (!accountId.equals(qualification.getMemberId())) {
            throw new ServiceException("无权使用该队长资格", 403);
        }
        return qualification;
    }

    @Override
    public BasketPlayerProfile requireCertifiedPlayer(Long accountId) {
        BasketPlayerProfile profile = playerProfileDao.selectOne(new LambdaQueryWrapper<BasketPlayerProfile>()
                .eq(BasketPlayerProfile::getMemberId, accountId)
                .eq(BasketPlayerProfile::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        if (profile == null) {
            throw new ServiceException("请先完成球员认证", 403);
        }
        if (!Objects.equals(PlayerCertStatusEnum.APPROVED.getCode(), profile.getCertificationStatus())) {
            throw new ServiceException("球员认证未通过", 403);
        }
        return profile;
    }

    @Override
    public BasketTeam requireTeamCaptain(Long accountId, Long teamId) {
        requireValidQualification(accountId);
        BasketTeam team = requireActiveTeam(teamId);
        if (!accountId.equals(team.getCaptainMemberId())) {
            throw new ServiceException("无权操作该球队", 403);
        }
        return team;
    }

    @Override
    public BasketTeamMember requireTeamMember(Long accountId, Long teamId) {
        BasketTeamMember member = teamMemberDao.selectOne(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getTeamId, teamId)
                .eq(BasketTeamMember::getMemberId, accountId)
                .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                .eq(BasketTeamMember::getMembershipStatus, TeamMembershipStatusEnum.ACTIVE.getCode())
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        if (member == null) {
            throw new ServiceException("您不是该球队成员", 403);
        }
        return member;
    }

    @Override
    public BasketTeam requireManageableTeam(Long accountId, Long teamId) {
        return requireTeamCaptain(accountId, teamId);
    }

    @Override
    public boolean hasCurrentTeamMembership(Long accountId) {
        Long count = teamMemberDao.selectCount(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getMemberId, accountId)
                .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                .eq(BasketTeamMember::getMembershipStatus, TeamMembershipStatusEnum.ACTIVE.getCode())
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED));
        return count != null && count > 0;
    }

    @Override
    public boolean hasActiveTeamMembershipOnPlatform(Long accountId, String platformCode) {
        return platformTeamMembershipSupport.hasActiveTeamMembership(accountId, platformCode);
    }

    @Override
    public boolean hasValidCaptainQualification(Long accountId) {
        CaptainQualificationDetailResp qualification = getCurrentQualification(accountId);
        return Boolean.TRUE.equals(qualification.getCanCreateTeam());
    }

    @Override
    public String resolveJoinIneligibilityReason(Long accountId) {
        for (String platformCode : PlatformTeamMembershipSupport.STANDARD_PLATFORMS) {
            if (resolveJoinIneligibilityReason(accountId, platformCode) == null) {
                return null;
            }
        }
        return "当前账号在所有平台均已加入球队，不能重复加入";
    }

    @Override
    public String resolveJoinIneligibilityReason(Long accountId, String platformCode) {
        return platformTeamMembershipSupport.resolveJoinIneligibilityReason(accountId, platformCode);
    }

    @Override
    public void assertEligibleToJoinTeam(Long accountId, String platformCode) {
        platformTeamMembershipSupport.assertEligibleToJoinTeam(accountId, platformCode);
    }

    @Override
    public BasketTournamentRegistration requireRegistrationCaptain(Long accountId, Long registrationId) {
        BasketTournamentRegistration registration = registrationDao.selectById(registrationId);
        if (registration == null || !NOT_DELETED.equals(registration.getIsDelete())) {
            throw new ServiceException("报名记录不存在");
        }
        if (!accountId.equals(registration.getCaptainMemberId())) {
            throw new ServiceException("无权操作该报名", 403);
        }
        requireTeamCaptain(accountId, registration.getTeamId());
        return registration;
    }

    private BasketCaptainQualification loadCurrentQualification(Long accountId) {
        return captainQualificationDao.selectOne(new LambdaQueryWrapper<BasketCaptainQualification>()
                .eq(BasketCaptainQualification::getMemberId, accountId)
                .eq(BasketCaptainQualification::getIsCurrent, IS_CURRENT)
                .eq(BasketCaptainQualification::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketCaptainQualification::getCreateTime)
                .orderByDesc(BasketCaptainQualification::getId)
                .last("limit 1"));
    }

    private String resolveInvalidReason(Long accountId, BasketCaptainQualification qualification) {
        Integer playerCertStatus = null;
        try {
            playerCertStatus = requireCertifiedPlayer(accountId).getCertificationStatus();
        } catch (ServiceException ex) {
            return ex.getMessage();
        }
        return CaptainQualificationValiditySupport.resolveInvalidReason(qualification, playerCertStatus, new Date());
    }

    private BasketTeam requireActiveTeam(Long teamId) {
        BasketTeam team = teamDao.selectById(teamId);
        if (team == null || !NOT_DELETED.equals(team.getIsDelete())) {
            throw new ServiceException("球队不存在");
        }
        if (!Objects.equals(TeamStatusEnum.ACTIVE.getCode(), team.getTeamStatus())) {
            throw new ServiceException("球队状态不可用");
        }
        return team;
    }
}
