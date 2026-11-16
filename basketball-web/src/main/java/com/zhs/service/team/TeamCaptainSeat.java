package com.zhs.service.team;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.dao.BasketTeamMemberDao;
import com.zhs.dao.v4.BasketCaptainGrantDao;
import com.zhs.enums.TeamMemberRoleEnum;
import com.zhs.enums.TeamMembershipStatusEnum;
import com.zhs.enums.v4.CaptainGrantStatusEnum;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamMember;
import com.zhs.model.v4.BasketCaptainGrant;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * 一支球队同时只认一个队长席位。席位字段优先；字段为空时，用本队仍有效的队长授权，再退到最早的一条队长成员记录。
 */
@Component
public class TeamCaptainSeat {

    private static final Integer NOT_DELETED = 0;
    private static final int IS_CURRENT = 1;

    @Resource
    private BasketTeamMemberDao teamMemberDao;
    @Resource
    private BasketCaptainGrantDao captainGrantDao;

    public Long memberId(BasketTeam team) {
        if (team == null) {
            return null;
        }
        if (team.getCaptainMemberId() != null) {
            return team.getCaptainMemberId();
        }
        return fallbackMemberId(team.getId());
    }

    public boolean holds(BasketTeam team, BasketTeamMember member) {
        if (team == null || member == null || !current(member)) {
            return false;
        }
        Long seatMemberId = memberId(team);
        return seatMemberId != null && seatMemberId.equals(member.getMemberId());
    }

    private Long fallbackMemberId(Long teamId) {
        if (teamId == null) {
            return null;
        }
        List<BasketCaptainGrant> grants = captainGrantDao.selectList(new LambdaQueryWrapper<BasketCaptainGrant>()
                .eq(BasketCaptainGrant::getTeamId, teamId)
                .eq(BasketCaptainGrant::getGrantStatus, CaptainGrantStatusEnum.ACTIVE.getCode())
                .eq(BasketCaptainGrant::getIsDelete, NOT_DELETED)
                .isNotNull(BasketCaptainGrant::getActiveUniqueKey)
                .orderByAsc(BasketCaptainGrant::getId));
        List<BasketTeamMember> members = teamMemberDao.selectList(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getTeamId, teamId)
                .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                .eq(BasketTeamMember::getMembershipStatus, TeamMembershipStatusEnum.ACTIVE.getCode())
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED)
                .orderByAsc(BasketTeamMember::getId));
        for (BasketTeamMember member : members) {
            if (!TeamMemberRoleEnum.CAPTAIN.getCode().equals(member.getRoleType()) || member.getMemberId() == null) {
                continue;
            }
            for (BasketCaptainGrant grant : grants) {
                if (member.getMemberId().equals(grant.getMemberId())) {
                    return member.getMemberId();
                }
            }
        }
        for (BasketTeamMember member : members) {
            if (TeamMemberRoleEnum.CAPTAIN.getCode().equals(member.getRoleType())) {
                return member.getMemberId();
            }
        }
        return null;
    }

    private boolean current(BasketTeamMember member) {
        return Objects.equals(member.getIsDelete(), NOT_DELETED)
                && Objects.equals(member.getIsCurrent(), IS_CURRENT)
                && Objects.equals(member.getMembershipStatus(), TeamMembershipStatusEnum.ACTIVE.getCode());
    }
}
