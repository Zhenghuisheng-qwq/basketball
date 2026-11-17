package com.zhs.service.team;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.BasketTeamDao;
import com.zhs.dao.BasketTeamMemberDao;
import com.zhs.dao.v4.BasketCaptainGrantDao;
import com.zhs.enums.TeamMembershipStatusEnum;
import com.zhs.enums.v4.CaptainGrantStatusEnum;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamMember;
import com.zhs.model.v4.BasketCaptainGrant;
import com.zhs.service.impl.v4.EventRosterMembershipSync;
import com.zhs.support.ExcludedRosterSupport;
import com.zhs.support.TeamMembershipSupport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 最终名单没有这支球队时，结束当前成员关系，腾出本届名额。
 * 队长席位一并让出；仍有效的队长授权只解除和这支球队的绑定，不撤销授权本身。
 */
@Component
public class ExcludedRosterRelease {

    private static final Integer NOT_DELETED = 0;
    private static final int IS_CURRENT = 1;

    @Resource
    private ExcludedRosterSupport excludedRosterSupport;
    @Resource
    private TeamMembershipSupport teamMembershipSupport;
    @Resource
    private EventRosterMembershipSync eventRosterMembershipSync;
    @Resource
    private BasketTeamDao teamDao;
    @Resource
    private BasketTeamMemberDao teamMemberDao;
    @Resource
    private BasketPlayerProfileDao playerProfileDao;
    @Resource
    private BasketCaptainGrantDao captainGrantDao;
    @Resource
    private TeamMembershipChangeGate membershipChangeGate;

    /**
     * @param joiningTeamId 正要加入的球队。已经在这支球队里时不结束关系。
     * @param grantToRefresh 建队用的授权对象。若绑定的就是被释放的球队，内存里的 teamId 一并清空。
     * @return 是否结束了一条「本届不参赛」的成员关系
     */
    public boolean releaseIfExcluded(Long memberId, Long divisionId, Long joiningTeamId, BasketCaptainGrant grantToRefresh) {
        if (memberId == null || divisionId == null) {
            return false;
        }
        eventRosterMembershipSync.lockDivision(divisionId);
        BasketTeamMember membership = teamMembershipSupport.findActiveMembership(memberId, divisionId);
        if (membership == null || membership.getTeamId() == null) {
            return false;
        }
        if (joiningTeamId != null && joiningTeamId.equals(membership.getTeamId())) {
            return false;
        }
        if (!excludedRosterSupport.isExcluded(membership.getTeamId(), divisionId)) {
            return false;
        }
        if (teamDao.lockById(membership.getTeamId()) == null) {
            return false;
        }
        BasketTeam team = teamDao.selectById(membership.getTeamId());
        if (team == null) {
            return false;
        }
        Date now = new Date();
        teamMemberDao.update(null, new LambdaUpdateWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getId, membership.getId())
                .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                .eq(BasketTeamMember::getMembershipStatus, TeamMembershipStatusEnum.ACTIVE.getCode())
                .set(BasketTeamMember::getMembershipStatus, TeamMembershipStatusEnum.LEFT.getCode())
                .set(BasketTeamMember::getIsCurrent, 0)
                .set(BasketTeamMember::getLeaveTime, now)
                .set(BasketTeamMember::getLeaveReason, "未进入正式名单后离队")
                .set(BasketTeamMember::getUpdateTime, now));
        teamMembershipSupport.clearActiveMemberSlot(membership);
        if (Objects.equals(team.getCaptainMemberId(), memberId)) {
            teamDao.update(null, new LambdaUpdateWrapper<BasketTeam>()
                    .eq(BasketTeam::getId, team.getId())
                    .set(BasketTeam::getCaptainMemberId, null)
                    .set(BasketTeam::getCaptainPlayerId, null)
                    .set(BasketTeam::getUpdateTime, now));
        }
        unbindGrant(memberId, divisionId, team.getId(), grantToRefresh);
        Long remain = teamMemberDao.selectCount(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getTeamId, team.getId())
                .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                .eq(BasketTeamMember::getMembershipStatus, TeamMembershipStatusEnum.ACTIVE.getCode())
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED));
        teamDao.update(null, new LambdaUpdateWrapper<BasketTeam>()
                .eq(BasketTeam::getId, team.getId())
                .set(BasketTeam::getCurrentMemberCount, remain == null ? 0 : remain.intValue())
                .set(BasketTeam::getUpdateTime, now));
        if (membership.getPlayerId() != null) {
            playerProfileDao.update(null, new LambdaUpdateWrapper<com.zhs.model.BasketPlayerProfile>()
                    .eq(com.zhs.model.BasketPlayerProfile::getId, membership.getPlayerId())
                    .eq(com.zhs.model.BasketPlayerProfile::getCurrentTeamId, team.getId())
                    .set(com.zhs.model.BasketPlayerProfile::getCurrentTeamId, null));
        }
        String rosterNote = eventRosterMembershipSync.releaseAfterVoluntaryLeave(team.getId(), membership.getPlayerId());
        String impact = "未进入正式名单，成员关系已结束，可以加入其他球队。";
        if (rosterNote != null && !rosterNote.isBlank()) {
            impact = impact + rosterNote;
        }
        membershipChangeGate.recordVoluntaryLeave(team, membership, impact);
        return true;
    }

    private void unbindGrant(Long memberId, Long divisionId, Long teamId, BasketCaptainGrant grantToRefresh) {
        List<BasketCaptainGrant> grants = captainGrantDao.selectList(new LambdaQueryWrapper<BasketCaptainGrant>()
                .eq(BasketCaptainGrant::getMemberId, memberId)
                .eq(BasketCaptainGrant::getDivisionId, divisionId)
                .eq(BasketCaptainGrant::getTeamId, teamId)
                .eq(BasketCaptainGrant::getGrantStatus, CaptainGrantStatusEnum.ACTIVE.getCode())
                .eq(BasketCaptainGrant::getIsDelete, NOT_DELETED));
        for (BasketCaptainGrant grant : grants) {
            captainGrantDao.update(null, new LambdaUpdateWrapper<BasketCaptainGrant>()
                    .eq(BasketCaptainGrant::getId, grant.getId())
                    .eq(BasketCaptainGrant::getGrantStatus, CaptainGrantStatusEnum.ACTIVE.getCode())
                    .eq(BasketCaptainGrant::getTeamId, teamId)
                    .set(BasketCaptainGrant::getTeamId, null)
                    .set(BasketCaptainGrant::getUpdateTime, new Date()));
            if (grantToRefresh != null && Objects.equals(grantToRefresh.getId(), grant.getId())) {
                grantToRefresh.setTeamId(null);
            }
        }
    }
}
