package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.dao.BasketCaptainQualificationDao;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.BasketTeamDao;
import com.zhs.dao.BasketTeamMemberDao;
import com.zhs.dao.BasketTournamentRegistrationDao;
import com.zhs.dao.BasketTournamentSeasonDao;
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
import com.zhs.model.BasketTournamentSeason;
import com.zhs.response.team.CaptainQualificationDetailResp;
import com.zhs.service.ICaptainPermissionService;
import com.zhs.support.CaptainGrantSupport;
import com.zhs.support.CaptainQualificationValiditySupport;
import com.zhs.support.TeamMembershipSupport;
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
    private BasketTournamentSeasonDao seasonDao;
    @Resource
    private TeamMembershipSupport teamMembershipSupport;
    @Resource
    private CaptainGrantSupport captainGrantSupport;

    @Override
    public CaptainQualificationDetailResp getCurrentQualification(Long accountId) {
        return getCurrentQualification(accountId, null);
    }

    @Override
    public CaptainQualificationDetailResp getCurrentQualification(Long accountId, Long seasonId) {
        return getCurrentQualification(accountId, seasonId, null);
    }

    @Override
    public CaptainQualificationDetailResp getCurrentQualification(Long accountId, Long seasonId, Long teamId) {
        BasketCaptainQualification qualification = seasonId != null
                ? captainGrantSupport.findActiveGrantForSeason(accountId, seasonId)
                : captainGrantSupport.findCurrentGrant(accountId);
        CaptainQualificationDetailResp resp = new CaptainQualificationDetailResp();
        if (qualification == null) {
            resp.setHasQualification(false);
            resp.setCanCreateTeam(false);
            resp.setRuntimeValid(false);
            resp.setInvalidReason(CaptainGrantSupport.NO_ACTIVE_GRANT_MESSAGE);
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
        if (qualification.getSeasonId() != null) {
            resp.setSeasonId(String.valueOf(qualification.getSeasonId()));
            BasketTournamentSeason season = seasonDao.selectById(qualification.getSeasonId());
            resp.setSeasonName(season == null ? null : season.getSeasonName());
        }
        if (qualification.getTeamId() != null) {
            resp.setTeamId(String.valueOf(qualification.getTeamId()));
            BasketTeam team = teamDao.selectById(qualification.getTeamId());
            resp.setTeamName(team == null ? null : team.getTeamName());
        }

        String invalidReason = resolveInvalidReason(accountId, qualification);
        resp.setInvalidReason(invalidReason);
        boolean runtimeValid = invalidReason == null;
        resp.setRuntimeValid(runtimeValid);
        resp.setGrantRevoked(!runtimeValid && qualification.getTeamId() != null);
        boolean unbound = qualification.getTeamId() == null;
        resp.setCanCreateTeam(runtimeValid && unbound);
        resp.setCanInheritTeam(runtimeValid && unbound);
        resp.setCanManageTeam(runtimeValid && qualification.getTeamId() != null);
        if (teamId != null) {
            BasketTeam team = teamDao.selectById(teamId);
            if (team == null || !NOT_DELETED.equals(team.getIsDelete())) {
                resp.setCanManageCurrentTeam(false);
                resp.setCannotManageReason("球队不存在");
            } else if (!runtimeValid) {
                resp.setCanManageCurrentTeam(false);
                resp.setCannotManageReason(invalidReason);
            } else {
                boolean canManage = canManageTeamByGrant(qualification, team, accountId);
                resp.setCanManageCurrentTeam(canManage);
                if (!canManage) {
                    resp.setCannotManageReason(resolveCannotManageReason(qualification, team, accountId));
                }
            }
        }
        return resp;
    }

    private boolean canManageTeamByGrant(BasketCaptainQualification grant, BasketTeam team, Long memberId) {
        try {
            captainGrantSupport.assertCanManageTeam(grant, team, memberId);
            return true;
        } catch (ServiceException ex) {
            return false;
        }
    }

    private String resolveCannotManageReason(BasketCaptainQualification grant, BasketTeam team, Long memberId) {
        if (grant == null) {
            return CaptainGrantSupport.NO_ACTIVE_GRANT_MESSAGE;
        }
        if (grant.getTeamId() != null && !Objects.equals(grant.getTeamId(), team.getId())) {
            return "当前队长授权已绑定其他球队，无法管理该球队";
        }
        if (grant.getTeamId() == null && !Objects.equals(team.getCaptainMemberId(), memberId)) {
            return "当前暂无有效队长权限，暂不能管理球队";
        }
        return "当前暂无有效队长权限，暂不能管理球队";
    }

    @Override
    public BasketCaptainQualification requireValidQualification(Long accountId) {
        return requireValidQualification(accountId, null);
    }

    @Override
    public BasketCaptainQualification requireValidQualification(Long accountId, Long seasonId) {
        BasketCaptainQualification qualification = seasonId != null
                ? captainGrantSupport.findActiveGrantForSeason(accountId, seasonId)
                : captainGrantSupport.findCurrentGrant(accountId);
        String invalidReason = resolveInvalidReason(accountId, qualification);
        if (invalidReason != null) {
            throw new ServiceException(invalidReason, 403);
        }
        if (!accountId.equals(qualification.getMemberId())) {
            throw new ServiceException("无权使用该队长授权", 403);
        }
        return qualification;
    }

    @Override
    public BasketCaptainQualification requireValidQualificationForEvent(Long accountId, Long teamId, Long seasonId) {
        requireCertifiedPlayer(accountId);
        if (seasonId == null) {
            throw new ServiceException("赛季主键不能为空");
        }
        BasketCaptainQualification qualification = captainGrantSupport.findActiveGrantForEvent(accountId, teamId, seasonId);
        if (qualification == null) {
            throw new ServiceException("当前球队暂未获得该赛事报名资格，请联系管理员确认缴费和授权", 403);
        }
        String invalidReason = resolveInvalidReason(accountId, qualification);
        if (invalidReason != null) {
            throw new ServiceException("当前球队暂未获得该赛事报名资格，请联系管理员确认缴费和授权", 403);
        }
        if (!Objects.equals(qualification.getSeasonId(), seasonId)) {
            throw new ServiceException("当前队长权限与所选赛事不匹配", 403);
        }
        if (qualification.getTeamId() != null && teamId != null && !Objects.equals(qualification.getTeamId(), teamId)) {
            throw new ServiceException("当前球队暂未获得该赛事报名资格，请联系管理员确认缴费和授权", 403);
        }
        if (!accountId.equals(qualification.getMemberId())) {
            throw new ServiceException("无权使用该队长授权", 403);
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
        BasketCaptainQualification grant = requireValidQualification(accountId);
        BasketTeam team = requireActiveTeam(teamId);
        captainGrantSupport.assertCanManageTeam(grant, team, accountId);
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
        return teamMembershipSupport.hasActiveTeamMembership(accountId);
    }

    @Override
    public boolean hasActiveTeamMembershipOnPlatform(Long accountId, String platformCode) {
        return teamMembershipSupport.hasActiveTeamMembership(accountId);
    }

    @Override
    public boolean hasValidCaptainQualification(Long accountId) {
        CaptainQualificationDetailResp qualification = getCurrentQualification(accountId);
        return Boolean.TRUE.equals(qualification.getRuntimeValid());
    }

    @Override
    public String resolveJoinIneligibilityReason(Long accountId) {
        return teamMembershipSupport.resolveJoinIneligibilityReason(accountId);
    }

    @Override
    public String resolveJoinIneligibilityReason(Long accountId, String platformCode) {
        return resolveJoinIneligibilityReason(accountId);
    }

    @Override
    public void assertEligibleToJoinTeam(Long accountId, String platformCode) {
        teamMembershipSupport.assertEligibleToJoinTeam(accountId);
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
        BasketCaptainQualification grant = requireValidQualificationForEvent(
                accountId, registration.getTeamId(), registration.getSeasonId());
        captainGrantSupport.assertGrantBoundToTeam(grant, registration.getTeamId());
        return registration;
    }

    private String resolveInvalidReason(Long accountId, BasketCaptainQualification qualification) {
        Integer playerCertStatus = null;
        try {
            playerCertStatus = requireCertifiedPlayer(accountId).getCertificationStatus();
        } catch (ServiceException ex) {
            return ex.getMessage();
        }
        Integer seasonStatus = null;
        if (qualification != null && qualification.getSeasonId() != null) {
            BasketTournamentSeason season = seasonDao.selectById(qualification.getSeasonId());
            if (season != null) {
                seasonStatus = season.getSeasonStatus();
            }
        }
        return CaptainQualificationValiditySupport.resolveInvalidReason(
                qualification, playerCertStatus, new Date(), seasonStatus);
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
