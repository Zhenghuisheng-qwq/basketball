package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.BasketTeamDao;
import com.zhs.dao.BasketTeamMemberDao;
import com.zhs.dao.v4.BasketDivisionDao;
import com.zhs.dao.v4.BasketSeasonDao;
import com.zhs.dao.v4.BasketSeriesDao;
import com.zhs.enums.PlayerCertStatusEnum;
import com.zhs.enums.TeamMembershipStatusEnum;
import com.zhs.enums.TeamStatusEnum;
import com.zhs.enums.v4.CaptainGrantStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamMember;
import com.zhs.model.v4.BasketCaptainGrant;
import com.zhs.model.v4.BasketDivision;
import com.zhs.model.v4.BasketSeason;
import com.zhs.model.v4.BasketSeries;
import com.zhs.response.team.CaptainQualificationDetailResp;
import com.zhs.service.ICaptainPermissionService;
import com.zhs.support.CaptainGrantSupport;
import com.zhs.support.CaptainGrantValiditySupport;
import com.zhs.support.TeamMembershipSupport;
import com.zhs.support.TeamWritableSupport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class CaptainPermissionServiceImpl implements ICaptainPermissionService {

    private static final Integer NOT_DELETED = 0;
    private static final int IS_CURRENT = 1;
    private static final int PAYMENT_CONFIRMED = 1;

    @Resource
    private BasketPlayerProfileDao playerProfileDao;
    @Resource
    private BasketTeamDao teamDao;
    @Resource
    private BasketTeamMemberDao teamMemberDao;
    @Resource
    private BasketSeasonDao seasonDao;
    @Resource
    private BasketDivisionDao divisionDao;
    @Resource
    private BasketSeriesDao seriesDao;
    @Resource
    private TeamMembershipSupport teamMembershipSupport;
    @Resource
    private CaptainGrantSupport captainGrantSupport;
    @Resource
    private TeamWritableSupport teamWritableSupport;

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
        BasketCaptainGrant grant = seasonId != null
                ? captainGrantSupport.findActiveGrantForSeason(accountId, seasonId)
                : captainGrantSupport.findCurrentGrant(accountId);
        return buildQualificationDetail(accountId, grant, teamId);
    }

    @Override
    public CaptainQualificationDetailResp getCurrentQualificationByDivision(Long accountId, Long divisionId, Long teamId) {
        BasketPlayerProfile player = findPlayerByMemberId(accountId);
        BasketCaptainGrant grant = player == null ? null
                : captainGrantSupport.findActiveGrantForDivision(accountId, divisionId, player.getId());
        return buildQualificationDetail(accountId, grant, teamId);
    }

    @Override
    public List<CaptainQualificationDetailResp> listActiveQualifications(Long accountId) {
        List<BasketCaptainGrant> grants = captainGrantSupport.findActiveGrantsForMember(accountId);
        List<CaptainQualificationDetailResp> rows = new ArrayList<>();
        for (BasketCaptainGrant grant : grants) {
            rows.add(buildQualificationDetail(accountId, grant, grant.getTeamId()));
        }
        return rows;
    }

    private CaptainQualificationDetailResp buildQualificationDetail(Long accountId, BasketCaptainGrant grant, Long teamId) {
        CaptainQualificationDetailResp resp = new CaptainQualificationDetailResp();
        if (grant == null) {
            resp.setHasQualification(false);
            resp.setCanCreateTeam(false);
            resp.setRuntimeValid(false);
            resp.setInvalidReason(CaptainGrantSupport.NO_ACTIVE_GRANT_MESSAGE);
            return resp;
        }

        CaptainGrantStatusEnum status = CaptainGrantStatusEnum.fromCode(grant.getGrantStatus());
        resp.setHasQualification(true);
        resp.setQualificationNo(String.valueOf(grant.getId()));
        resp.setStatus(mapGrantStatusToLegacyCode(grant.getGrantStatus()));
        resp.setStatusText(status.getDescription());
        resp.setPaymentConfirmed(PAYMENT_CONFIRMED);
        resp.setValidFrom(grant.getValidFrom());
        resp.setValidTo(grant.getValidTo());
        resp.setIsCurrent(grant.getActiveUniqueKey() != null ? IS_CURRENT : 0);
        if (grant.getDivisionId() != null) {
            resp.setDivisionId(String.valueOf(grant.getDivisionId()));
            BasketDivision division = divisionDao.selectById(grant.getDivisionId());
            if (division != null && Objects.equals(NOT_DELETED, division.getIsDelete())) {
                resp.setDivisionName(division.getDivisionName());
                resp.setPlatformCode(division.getPlatformCode());
                resp.setGameMode(division.getGameMode());
            }
        }
        if (grant.getSeasonId() != null) {
            resp.setSeasonId(String.valueOf(grant.getSeasonId()));
            BasketSeason season = seasonDao.selectById(grant.getSeasonId());
            if (season != null) {
                resp.setSeasonName(season.getDisplayTitle() != null ? season.getDisplayTitle() : season.getSeasonName());
                if (season.getSeriesId() != null) {
                    BasketSeries series = seriesDao.selectById(season.getSeriesId());
                    if (series != null) {
                        resp.setSeriesName(series.getSeriesName());
                    }
                }
            }
        }
        if (grant.getTeamId() != null) {
            resp.setTeamId(String.valueOf(grant.getTeamId()));
            BasketTeam team = teamDao.selectById(grant.getTeamId());
            resp.setTeamName(team == null ? null : team.getTeamName());
        }

        String invalidReason = resolveInvalidReason(accountId, grant);
        resp.setInvalidReason(invalidReason);
        boolean runtimeValid = invalidReason == null;
        resp.setRuntimeValid(runtimeValid);
        resp.setGrantRevoked(!runtimeValid && CaptainGrantStatusEnum.REVOKED.getCode().equals(grant.getGrantStatus()));
        boolean canCreateByGrant = runtimeValid
                && grant.getTeamId() == null
                && grant.getDivisionId() != null
                && !teamMembershipSupport.hasActiveTeamMembership(accountId, grant.getDivisionId());
        resp.setCanCreateTeam(canCreateByGrant);
        // 历史球队复活已废弃
        resp.setCanInheritTeam(false);
        resp.setCanManageTeam(runtimeValid && grant.getTeamId() != null);
        if (teamId != null) {
            BasketTeam team = teamDao.selectById(teamId);
            if (team == null || !NOT_DELETED.equals(team.getIsDelete())) {
                resp.setCanManageCurrentTeam(false);
                resp.setCannotManageReason("球队不存在");
            } else if (!runtimeValid) {
                resp.setCanManageCurrentTeam(false);
                resp.setCannotManageReason(invalidReason);
            } else {
                boolean canManage = canManageTeamByGrant(grant, team, accountId);
                resp.setCanManageCurrentTeam(canManage);
                if (!canManage) {
                    resp.setCannotManageReason(resolveCannotManageReason(grant, team, accountId));
                }
            }
        }
        return resp;
    }

    private Integer mapGrantStatusToLegacyCode(String grantStatus) {
        if (CaptainGrantStatusEnum.ACTIVE.getCode().equals(grantStatus)) {
            return 1;
        }
        if (CaptainGrantStatusEnum.REVOKED.getCode().equals(grantStatus)) {
            return 2;
        }
        if (CaptainGrantStatusEnum.EXPIRED.getCode().equals(grantStatus)) {
            return 3;
        }
        return null;
    }

    private boolean canManageTeamByGrant(BasketCaptainGrant grant, BasketTeam team, Long memberId) {
        try {
            captainGrantSupport.assertCanManageTeam(grant, team, memberId);
            return true;
        } catch (ServiceException ex) {
            return false;
        }
    }

    private String resolveCannotManageReason(BasketCaptainGrant grant, BasketTeam team, Long memberId) {
        if (grant == null) {
            return CaptainGrantSupport.NO_ACTIVE_GRANT_MESSAGE;
        }
        if (grant.getTeamId() != null && !Objects.equals(grant.getTeamId(), team.getId())) {
            return CaptainGrantSupport.GRANT_BOUND_OTHER_TEAM_MESSAGE;
        }
        if (!Objects.equals(team.getCaptainMemberId(), memberId)) {
            return "无权操作该球队";
        }
        return CaptainGrantSupport.NO_ACTIVE_GRANT_MESSAGE;
    }

    @Override
    public BasketCaptainGrant requireValidQualification(Long accountId) {
        return requireValidQualification(accountId, null);
    }

    @Override
    public BasketCaptainGrant requireValidQualification(Long accountId, Long seasonId) {
        BasketCaptainGrant grant = seasonId != null
                ? captainGrantSupport.findActiveGrantForSeason(accountId, seasonId)
                : captainGrantSupport.findCurrentGrant(accountId);
        return requireRuntimeValidGrant(accountId, grant);
    }

    @Override
    public BasketCaptainGrant requireValidQualificationForDivision(Long accountId, Long teamId, Long divisionId) {
        BasketPlayerProfile player = requireCertifiedPlayer(accountId);
        if (divisionId == null) {
            throw new ServiceException("Division 主键不能为空");
        }
        BasketCaptainGrant grant = captainGrantSupport.findActiveGrantForDivision(accountId, divisionId, player.getId());
        if (grant == null) {
            throw new ServiceException("当前球队暂未获得该赛事报名资格，请联系管理员确认缴费和授权", 403);
        }
        requireRuntimeValidGrant(accountId, grant);
        if (teamId != null) {
            captainGrantSupport.assertCanManageTeam(grant, requireActiveTeam(teamId), accountId);
        }
        return grant;
    }

    @Override
    public BasketCaptainGrant requireValidQualificationForEvent(Long accountId, Long teamId, Long seasonId) {
        requireCertifiedPlayer(accountId);
        if (seasonId == null) {
            throw new ServiceException("赛季主键不能为空");
        }
        BasketCaptainGrant grant = captainGrantSupport.findActiveGrantForSeason(accountId, seasonId);
        if (grant == null) {
            throw new ServiceException("当前球队暂未获得该赛事报名资格，请联系管理员确认缴费和授权", 403);
        }
        requireRuntimeValidGrant(accountId, grant);
        if (teamId != null) {
            captainGrantSupport.assertCanManageTeam(grant, requireActiveTeam(teamId), accountId);
        }
        return grant;
    }

    private BasketCaptainGrant requireRuntimeValidGrant(Long accountId, BasketCaptainGrant grant) {
        String invalidReason = resolveInvalidReason(accountId, grant);
        if (invalidReason != null) {
            throw new ServiceException(invalidReason, 403);
        }
        if (grant == null || !accountId.equals(grant.getMemberId())) {
            throw new ServiceException("无权使用该队长授权", 403);
        }
        return grant;
    }

    @Override
    public BasketPlayerProfile requireCertifiedPlayer(Long accountId) {
        BasketPlayerProfile profile = findPlayerByMemberId(accountId);
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
        BasketTeam team = requireActiveTeam(teamId);
        teamWritableSupport.assertTeamWritable(team);
        BasketPlayerProfile player = requireCertifiedPlayer(accountId);
        BasketCaptainGrant grant = captainGrantSupport.findActiveGrantForDivision(
                accountId, team.getDivisionId(), player.getId());
        requireRuntimeValidGrant(accountId, grant);
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
    public BasketTeamMember requireAnyTeamMembership(Long accountId, Long teamId) {
        BasketTeamMember active = teamMemberDao.selectOne(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getTeamId, teamId)
                .eq(BasketTeamMember::getMemberId, accountId)
                .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                .eq(BasketTeamMember::getMembershipStatus, TeamMembershipStatusEnum.ACTIVE.getCode())
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        if (active != null) {
            return active;
        }
        BasketTeamMember any = teamMemberDao.selectOne(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getTeamId, teamId)
                .eq(BasketTeamMember::getMemberId, accountId)
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketTeamMember::getJoinTime)
                .orderByDesc(BasketTeamMember::getId)
                .last("limit 1"));
        if (any == null) {
            throw new ServiceException("您不是该球队成员", 403);
        }
        return any;
    }

    @Override
    public boolean canManageTeam(Long accountId, Long teamId) {
        try {
            requireTeamCaptain(accountId, teamId);
            return true;
        } catch (ServiceException ex) {
            return false;
        }
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
        throw new ServiceException("请按具体赛事 Division 校验入队资格", 400);
    }

    public void assertEligibleToJoinTeamInDivision(Long accountId, Long divisionId) {
        teamMembershipSupport.assertEligibleToJoinTeam(accountId, divisionId);
    }

    private String resolveInvalidReason(Long accountId, BasketCaptainGrant grant) {
        Integer playerCertStatus = null;
        try {
            playerCertStatus = requireCertifiedPlayer(accountId).getCertificationStatus();
        } catch (ServiceException ex) {
            return ex.getMessage();
        }
        String seasonStatus = null;
        if (grant != null && grant.getSeasonId() != null) {
            BasketSeason season = seasonDao.selectById(grant.getSeasonId());
            if (season != null) {
                seasonStatus = season.getSeasonStatus();
            }
        }
        return CaptainGrantValiditySupport.resolveInvalidReason(
                grant, playerCertStatus, new Date(), seasonStatus);
    }

    private BasketPlayerProfile findPlayerByMemberId(Long memberId) {
        return playerProfileDao.selectOne(new LambdaQueryWrapper<BasketPlayerProfile>()
                .eq(BasketPlayerProfile::getMemberId, memberId)
                .eq(BasketPlayerProfile::getIsDelete, NOT_DELETED)
                .last("limit 1"));
    }

    private BasketTeam requireActiveTeam(Long teamId) {
        BasketTeam team = teamDao.selectById(teamId);
        if (team == null || !NOT_DELETED.equals(team.getIsDelete())) {
            throw new ServiceException("球队不存在");
        }
        if (!Objects.equals(TeamStatusEnum.ACTIVE.getCode(), team.getTeamStatus())) {
            throw new ServiceException("球队状态不可用");
        }
        if (team.getDivisionId() == null) {
            throw new ServiceException(TeamWritableSupport.LEGACY_TEAM_READONLY_MESSAGE, 403);
        }
        return team;
    }
}
