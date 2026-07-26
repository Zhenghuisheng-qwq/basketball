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
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamMember;
import com.zhs.service.IBusinessMessageService;
import com.zhs.support.GameTerminalSupport;
import jakarta.annotation.Resource;
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

    @Transactional(rollbackFor = Exception.class)
    public BasketTeamMember addMember(BasketTeam team, Long memberId, Long playerId, TeamJoinSourceEnum joinSource) {
        assertTeamHasCapacity(team);
        assertNoCurrentMembership(memberId);
        assertPlayerTeamTerminalMatch(team, playerId);

        Date now = new Date();
        BasketTeamMember member = new BasketTeamMember();
        member.setMembershipNo("MB" + IdUtil.getSnowflakeNextIdStr());
        member.setTeamId(team.getId());
        member.setMemberId(memberId);
        member.setPlayerId(playerId);
        member.setRoleType(TeamMemberRoleEnum.MEMBER.getCode());
        member.setRosterType(TeamRosterTypeEnum.OFFICIAL.getCode());
        member.setJoinSource(joinSource.getCode());
        member.setMembershipStatus(TeamMembershipStatusEnum.ACTIVE.getCode());
        member.setIsCurrent(IS_CURRENT);
        member.setJoinTime(now);
        member.setIsDelete(NOT_DELETED);
        teamMemberDao.insert(member);

        recalculateCurrentMemberCount(team.getId());
        updatePlayerCurrentTeam(playerId, team.getId());
        closePendingInvitesAndApplications(memberId, team.getId());
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

    private void assertPlayerTeamTerminalMatch(BasketTeam team, Long playerId) {
        BasketPlayerProfile profile = playerProfileDao.selectById(playerId);
        if (profile == null || !NOT_DELETED.equals(profile.getIsDelete())) {
            throw new ServiceException("球员档案不存在");
        }
        String reason = GameTerminalSupport.resolvePlayerTeamMismatchReason(profile, team);
        if (reason != null) {
            throw new ServiceException(reason);
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

    private void assertNoCurrentMembership(Long memberId) {
        Long count = teamMemberDao.selectCount(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getMemberId, memberId)
                .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                .eq(BasketTeamMember::getMembershipStatus, TeamMembershipStatusEnum.ACTIVE.getCode())
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED));
        if (count != null && count > 0) {
            throw new ServiceException("该球员已在其他球队中");
        }
    }

    private void updatePlayerCurrentTeam(Long playerId, Long teamId) {
        BasketPlayerProfile profile = playerProfileDao.selectById(playerId);
        if (profile != null && NOT_DELETED.equals(profile.getIsDelete())) {
            profile.setCurrentTeamId(teamId);
            playerProfileDao.updateById(profile);
        }
    }

    private void closePendingInvitesAndApplications(Long memberId, Long teamId) {
        Date now = new Date();
        invitationDao.update(null, new LambdaUpdateWrapper<com.zhs.model.BasketTeamInvitation>()
                .eq(com.zhs.model.BasketTeamInvitation::getInviteeMemberId, memberId)
                .eq(com.zhs.model.BasketTeamInvitation::getInvitationStatus, TeamInvitationStatusEnum.PENDING.getCode())
                .eq(com.zhs.model.BasketTeamInvitation::getIsDelete, NOT_DELETED)
                .set(com.zhs.model.BasketTeamInvitation::getInvitationStatus, TeamInvitationStatusEnum.CANCELLED.getCode())
                .set(com.zhs.model.BasketTeamInvitation::getRespondedTime, now));

        joinApplicationDao.update(null, new LambdaUpdateWrapper<com.zhs.model.BasketTeamJoinApplication>()
                .eq(com.zhs.model.BasketTeamJoinApplication::getApplicantMemberId, memberId)
                .eq(com.zhs.model.BasketTeamJoinApplication::getApplicationStatus, TeamJoinApplicationStatusEnum.PENDING.getCode())
                .eq(com.zhs.model.BasketTeamJoinApplication::getIsDelete, NOT_DELETED)
                .set(com.zhs.model.BasketTeamJoinApplication::getApplicationStatus, TeamJoinApplicationStatusEnum.CANCELLED.getCode())
                .set(com.zhs.model.BasketTeamJoinApplication::getProcessedTime, now));
    }
}
