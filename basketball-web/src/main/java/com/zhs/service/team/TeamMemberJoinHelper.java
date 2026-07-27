package com.zhs.service.team;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.BasketTeamDao;
import com.zhs.dao.BasketTeamJoinApplicationDao;
import com.zhs.dao.BasketTeamInvitationDao;
import com.zhs.dao.BasketTeamMemberDao;
import com.zhs.enums.TeamJoinApplicationStatusEnum;
import com.zhs.enums.TeamInvitationStatusEnum;
import com.zhs.enums.TeamJoinSourceEnum;
import com.zhs.enums.TeamMemberRoleEnum;
import com.zhs.enums.TeamMembershipStatusEnum;
import com.zhs.enums.TeamRosterTypeEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketPlayerPlatformAccount;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamMember;
import com.zhs.service.IBusinessMessageService;
import com.zhs.support.GameTerminalSupport;
import com.zhs.support.PlatformTeamMembershipSupport;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Component
public class TeamMemberJoinHelper {

    private static final Integer NOT_DELETED = 0;
    private static final int IS_CURRENT = 1;
    private static final String BIZ_TYPE_TEAM_MEMBER = "TEAM_MEMBER";

    @Resource
    private BasketTeamDao teamDao;
    @Resource
    private BasketTeamMemberDao teamMemberDao;
    @Resource
    private BasketTeamInvitationDao invitationDao;
    @Resource
    private BasketTeamJoinApplicationDao joinApplicationDao;
    @Resource
    private BasketPlayerProfileDao playerProfileDao;
    @Resource
    private IBusinessMessageService businessMessageService;
    @Resource
    private PlatformTeamMembershipSupport platformTeamMembershipSupport;

    @Transactional(rollbackFor = Exception.class)
    public BasketTeamMember addMember(BasketTeam team, Long memberId, Long playerId, TeamJoinSourceEnum joinSource) {
        assertTeamHasCapacity(team);
        String platformCode = platformTeamMembershipSupport.resolveTeamPlatformCode(team);
        platformTeamMembershipSupport.assertNoActiveMembershipOnPlatform(memberId, platformCode);
        BasketPlayerPlatformAccount platformIdentity = platformTeamMembershipSupport
                .requireActivePlatformIdentity(memberId, platformCode);
        platformTeamMembershipSupport.assertPlatformIdentityMatchesTeam(platformIdentity, team);
        assertPlayerTeamTerminalMatch(team, playerId, platformCode);

        Date now = new Date();
        BasketTeamMember member = new BasketTeamMember();
        member.setMembershipNo("MB" + IdUtil.getSnowflakeNextIdStr());
        member.setTeamId(team.getId());
        member.setMemberId(memberId);
        member.setPlayerId(playerId);
        member.setPlatformCode(platformCode);
        member.setPlatformIdentityId(platformIdentity.getId());
        member.setActivePlatformSlot(platformTeamMembershipSupport.buildActivePlatformSlot(memberId, platformCode));
        member.setRoleType(TeamMemberRoleEnum.MEMBER.getCode());
        member.setRosterType(TeamRosterTypeEnum.OFFICIAL.getCode());
        member.setJoinSource(joinSource.getCode());
        member.setMembershipStatus(TeamMembershipStatusEnum.ACTIVE.getCode());
        member.setIsCurrent(IS_CURRENT);
        member.setJoinTime(now);
        member.setIsDelete(NOT_DELETED);
        try {
            teamMemberDao.insert(member);
        } catch (DuplicateKeyException ex) {
            throw new ServiceException("当前账号在该平台已加入球队，不能重复加入");
        }

        recalculateCurrentMemberCount(team.getId());
        updatePlayerCurrentTeam(playerId, team.getId(), platformCode);
        closePendingInvitesAndApplications(memberId, team.getId(), platformCode);
        businessMessageService.sendSiteMessage(memberId, "加入球队成功",
                "您已成功加入球队「" + team.getTeamName() + "」",
                BIZ_TYPE_TEAM_MEMBER, team.getId(), "TEAM", String.valueOf(team.getId()));
        return member;
    }

    public void recalculateCurrentMemberCount(Long teamId) {
        teamDao.selectById(teamId);
        Long count = teamMemberDao.selectCount(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getTeamId, teamId)
                .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                .eq(BasketTeamMember::getMembershipStatus, TeamMembershipStatusEnum.ACTIVE.getCode())
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED));
        BasketTeam update = new BasketTeam();
        update.setId(teamId);
        update.setCurrentMemberCount(count == null ? 0 : count.intValue());
        teamDao.updateById(update);
    }

    public void clearActivePlatformSlot(BasketTeamMember member) {
        if (member == null || member.getId() == null) {
            return;
        }
        BasketTeamMember update = new BasketTeamMember();
        update.setId(member.getId());
        update.setActivePlatformSlot(null);
        teamMemberDao.updateById(update);
    }

    private void assertPlayerTeamTerminalMatch(BasketTeam team, Long playerId, String platformCode) {
        BasketPlayerProfile profile = playerProfileDao.selectById(playerId);
        if (profile == null || !NOT_DELETED.equals(profile.getIsDelete())) {
            throw new ServiceException("球员档案不存在");
        }
        if (!GameTerminalSupport.matches(platformCode, team.getGameTerminal())) {
            throw new ServiceException("平台游戏身份与球队平台不一致，无法加入");
        }
    }

    private void assertTeamHasCapacity(BasketTeam team) {
        recalculateCurrentMemberCount(team.getId());
        BasketTeam latest = teamDao.selectById(team.getId());
        if (latest.getMaxMemberCount() != null && latest.getCurrentMemberCount() != null
                && latest.getCurrentMemberCount() >= latest.getMaxMemberCount()) {
            throw new ServiceException("球队成员已满");
        }
    }

    private void updatePlayerCurrentTeam(Long playerId, Long teamId, String platformCode) {
        BasketPlayerProfile profile = playerProfileDao.selectById(playerId);
        if (profile != null && NOT_DELETED.equals(profile.getIsDelete())
                && GameTerminalSupport.matches(platformCode, profile.getGameTerminal())) {
            profile.setCurrentTeamId(teamId);
            playerProfileDao.updateById(profile);
        }
    }

    private void closePendingInvitesAndApplications(Long memberId, Long teamId, String platformCode) {
        Date now = new Date();
        List<com.zhs.model.BasketTeamInvitation> pendingInvites = invitationDao.selectList(
                new LambdaQueryWrapper<com.zhs.model.BasketTeamInvitation>()
                        .eq(com.zhs.model.BasketTeamInvitation::getInviteeMemberId, memberId)
                        .eq(com.zhs.model.BasketTeamInvitation::getInvitationStatus, TeamInvitationStatusEnum.PENDING.getCode())
                        .eq(com.zhs.model.BasketTeamInvitation::getIsDelete, NOT_DELETED));
        for (com.zhs.model.BasketTeamInvitation invitation : pendingInvites) {
            if (teamId.equals(invitation.getTeamId())) {
                continue;
            }
            BasketTeam inviteTeam = teamDao.selectById(invitation.getTeamId());
            if (inviteTeam != null && platformCode.equals(
                    platformTeamMembershipSupport.normalizePlatformCode(inviteTeam.getGameTerminal()))) {
                invitationDao.update(null, new LambdaUpdateWrapper<com.zhs.model.BasketTeamInvitation>()
                        .eq(com.zhs.model.BasketTeamInvitation::getId, invitation.getId())
                        .set(com.zhs.model.BasketTeamInvitation::getInvitationStatus, TeamInvitationStatusEnum.CANCELLED.getCode())
                        .set(com.zhs.model.BasketTeamInvitation::getRespondedTime, now));
            }
        }

        List<com.zhs.model.BasketTeamJoinApplication> pendingApplications = joinApplicationDao.selectList(
                new LambdaQueryWrapper<com.zhs.model.BasketTeamJoinApplication>()
                        .eq(com.zhs.model.BasketTeamJoinApplication::getApplicantMemberId, memberId)
                        .eq(com.zhs.model.BasketTeamJoinApplication::getApplicationStatus, TeamJoinApplicationStatusEnum.PENDING.getCode())
                        .eq(com.zhs.model.BasketTeamJoinApplication::getIsDelete, NOT_DELETED));
        for (com.zhs.model.BasketTeamJoinApplication application : pendingApplications) {
            if (teamId.equals(application.getTeamId())) {
                continue;
            }
            BasketTeam applyTeam = teamDao.selectById(application.getTeamId());
            if (applyTeam != null && platformCode.equals(
                    platformTeamMembershipSupport.normalizePlatformCode(applyTeam.getGameTerminal()))) {
                joinApplicationDao.update(null, new LambdaUpdateWrapper<com.zhs.model.BasketTeamJoinApplication>()
                        .eq(com.zhs.model.BasketTeamJoinApplication::getId, application.getId())
                        .set(com.zhs.model.BasketTeamJoinApplication::getApplicationStatus, TeamJoinApplicationStatusEnum.CANCELLED.getCode())
                        .set(com.zhs.model.BasketTeamJoinApplication::getProcessedTime, now));
            }
        }
    }
}
