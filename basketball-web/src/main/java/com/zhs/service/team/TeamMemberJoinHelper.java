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
import com.zhs.enums.TeamStatusEnum;
import com.zhs.exception.BusinessReason;
import com.zhs.i18n.NoticeParams;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamMember;
import com.zhs.service.ICaptainPermissionService;
import com.zhs.service.IBusinessMessageService;
import com.zhs.service.impl.v4.EventRosterMembershipSync;
import com.zhs.support.TeamMembershipSupport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;

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
    private TeamMembershipSupport teamMembershipSupport;
    @Resource
    private ICaptainPermissionService captainPermissionService;
    @Resource
    private EventRosterMembershipSync eventRosterMembershipSync;

    @Transactional(rollbackFor = Exception.class)
    public BasketTeamMember addMember(BasketTeam team, Long memberId, Long playerId, TeamJoinSourceEnum joinSource) {
        if (team == null || team.getDivisionId() == null) {
            throw new ServiceException("历史球队不能加入新成员，请在当前赛事重新创建球队", 403);
        }
        // 先锁 Division，再改球队行，避免和锁定名单/离队的 Division→球队顺序死锁。
        eventRosterMembershipSync.lockDivision(team.getDivisionId());
        if (!Objects.equals(TeamStatusEnum.ACTIVE.getCode(), team.getTeamStatus())) {
            throw new ServiceException("当前球队不可加入", 403);
        }
        assertTeamHasCapacity(team);
        captainPermissionService.requireCertifiedPlayer(memberId);
        teamMembershipSupport.assertEligibleToJoinTeam(memberId, team.getDivisionId());
        eventRosterMembershipSync.retireStaleBeforeRejoin(team.getId(), playerId);

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
        teamMembershipSupport.applyActiveMemberSlot(member, team.getDivisionId(), memberId);
        teamMembershipSupport.insertMember(member);

        recalculateCurrentMemberCount(team.getId());
        // current_team_id 仅作最近展示兼容字段
        updatePlayerCurrentTeam(playerId, team.getId());
        closePendingInvitesAndApplications(memberId, team.getId());
        eventRosterMembershipSync.syncTeam(team.getId());
        businessMessageService.sendSiteMessage(memberId, "加入球队成功",
                "您已成功加入球队「" + team.getTeamName() + "」",
                BIZ_TYPE_TEAM_MEMBER, team.getId(), "TEAM", String.valueOf(team.getId()),
                "TEAM_JOIN_SUCCEEDED", NoticeParams.json("teamName", team.getTeamName()));
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

    public void clearActiveMemberSlot(BasketTeamMember member) {
        teamMembershipSupport.clearActiveMemberSlot(member);
    }

    private void updatePlayerCurrentTeam(Long playerId, Long teamId) {
        BasketPlayerProfile profile = playerProfileDao.selectById(playerId);
        if (profile != null && NOT_DELETED.equals(profile.getIsDelete())) {
            profile.setCurrentTeamId(teamId);
            playerProfileDao.updateById(profile);
        }
    }

    private void closePendingInvitesAndApplications(Long memberId, Long teamId) {
        BasketTeam joinedTeam = teamDao.selectById(teamId);
        Long divisionId = joinedTeam == null ? null : joinedTeam.getDivisionId();
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
            // 仅关闭同 Division 的待处理邀请，跨 Division 邀请保留
            if (!isSameDivisionTeam(invitation.getTeamId(), divisionId)) {
                continue;
            }
            invitationDao.update(null, new LambdaUpdateWrapper<com.zhs.model.BasketTeamInvitation>()
                    .eq(com.zhs.model.BasketTeamInvitation::getId, invitation.getId())
                    .set(com.zhs.model.BasketTeamInvitation::getInvitationStatus, TeamInvitationStatusEnum.CANCELLED.getCode())
                    .set(com.zhs.model.BasketTeamInvitation::getRespondedTime, now));
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
            if (!isSameDivisionTeam(application.getTeamId(), divisionId)) {
                continue;
            }
            joinApplicationDao.update(null, new LambdaUpdateWrapper<com.zhs.model.BasketTeamJoinApplication>()
                    .eq(com.zhs.model.BasketTeamJoinApplication::getId, application.getId())
                    .set(com.zhs.model.BasketTeamJoinApplication::getApplicationStatus, TeamJoinApplicationStatusEnum.CANCELLED.getCode())
                    .set(com.zhs.model.BasketTeamJoinApplication::getProcessedTime, now));
        }
    }

    private boolean isSameDivisionTeam(Long otherTeamId, Long divisionId) {
        if (otherTeamId == null || divisionId == null) {
            return false;
        }
        BasketTeam other = teamDao.selectById(otherTeamId);
        return other != null && Objects.equals(other.getDivisionId(), divisionId);
    }

    private void assertTeamHasCapacity(BasketTeam team) {
        recalculateCurrentMemberCount(team.getId());
        BasketTeam latest = teamDao.selectById(team.getId());
        if (latest.getMaxMemberCount() != null && latest.getCurrentMemberCount() != null
                && latest.getCurrentMemberCount() >= latest.getMaxMemberCount()) {
            throw BusinessReason.TEAM_FULL.exception();
        }
    }
}
