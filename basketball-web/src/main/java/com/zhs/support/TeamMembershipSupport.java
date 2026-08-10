package com.zhs.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhs.dao.BasketTeamJoinApplicationDao;
import com.zhs.dao.BasketTeamMemberDao;
import com.zhs.enums.TeamJoinApplicationStatusEnum;
import com.zhs.enums.TeamMembershipStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketTeamJoinApplication;
import com.zhs.model.BasketTeamMember;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

/**
 * 球队成员关系：同 Division 一人一队；不同 Division 可并行加入。
 */
@Component
public class TeamMembershipSupport {

    private static final Integer NOT_DELETED = 0;
    private static final int IS_CURRENT = 1;

    @Resource
    private BasketTeamMemberDao teamMemberDao;
    @Resource
    private BasketTeamJoinApplicationDao joinApplicationDao;

    /** 兼容旧调用：取任意一条 ACTIVE（不保证 Division）。新业务请用 division 重载。 */
    public BasketTeamMember findActiveMembership(Long memberId) {
        List<BasketTeamMember> memberships = listActiveMemberships(memberId);
        return CollectionUtils.isEmpty(memberships) ? null : memberships.get(0);
    }

    public List<BasketTeamMember> listActiveMemberships(Long memberId) {
        if (memberId == null) {
            return Collections.emptyList();
        }
        return teamMemberDao.selectList(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getMemberId, memberId)
                .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                .eq(BasketTeamMember::getMembershipStatus, TeamMembershipStatusEnum.ACTIVE.getCode())
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketTeamMember::getJoinTime)
                .orderByDesc(BasketTeamMember::getId));
    }

    public BasketTeamMember findActiveMembership(Long memberId, Long divisionId) {
        if (memberId == null || divisionId == null) {
            return null;
        }
        return teamMemberDao.selectOne(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getMemberId, memberId)
                .eq(BasketTeamMember::getDivisionId, divisionId)
                .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                .eq(BasketTeamMember::getMembershipStatus, TeamMembershipStatusEnum.ACTIVE.getCode())
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketTeamMember::getJoinTime)
                .orderByDesc(BasketTeamMember::getId)
                .last("limit 1"));
    }

    public boolean hasActiveTeamMembership(Long memberId) {
        return findActiveMembership(memberId) != null;
    }

    public boolean hasActiveTeamMembership(Long memberId, Long divisionId) {
        return findActiveMembership(memberId, divisionId) != null;
    }

    /** @deprecated 全局一人一队语义已废弃，请使用 division 重载 */
    @Deprecated
    public String resolveJoinIneligibilityReason(Long memberId) {
        BasketTeamMember membership = findActiveMembership(memberId);
        if (membership != null) {
            return "当前账号已加入球队，不能重复加入";
        }
        return null;
    }

    public String resolveJoinIneligibilityReason(Long memberId, Long divisionId) {
        if (divisionId == null) {
            return TeamWritableSupport.LEGACY_TEAM_READONLY_MESSAGE;
        }
        BasketTeamMember membership = findActiveMembership(memberId, divisionId);
        if (membership != null) {
            return "当前账号已在该赛事加入球队，不能重复加入";
        }
        return null;
    }

    /** @deprecated 请使用 {@link #assertEligibleToJoinTeam(Long, Long)} */
    @Deprecated
    public void assertEligibleToJoinTeam(Long memberId) {
        String reason = resolveJoinIneligibilityReason(memberId);
        if (reason != null) {
            throw new ServiceException(reason, 403);
        }
    }

    public void assertEligibleToJoinTeam(Long memberId, Long divisionId) {
        String reason = resolveJoinIneligibilityReason(memberId, divisionId);
        if (reason != null) {
            throw new ServiceException(reason, 403);
        }
    }

    /** @deprecated Legacy slot：仅兼容旧数据读取，新写入请用 division 重载 */
    @Deprecated
    public String buildActiveMemberSlot(Long memberId) {
        if (memberId == null) {
            throw new ServiceException("会员主键不能为空");
        }
        return String.valueOf(memberId);
    }

    public String buildActiveMemberSlot(Long divisionId, Long memberId) {
        if (divisionId == null) {
            throw new ServiceException("Division 主键不能为空");
        }
        if (memberId == null) {
            throw new ServiceException("会员主键不能为空");
        }
        return divisionId + "_" + memberId;
    }

    /** @deprecated 请使用 {@link #applyActiveMemberSlot(BasketTeamMember, Long, Long)} */
    @Deprecated
    public void applyActiveMemberSlot(BasketTeamMember member, Long memberId) {
        member.setActiveMemberSlot(buildActiveMemberSlot(memberId));
        member.setActivePlatformSlot(null);
    }

    public void applyActiveMemberSlot(BasketTeamMember member, Long divisionId, Long memberId) {
        if (member == null) {
            return;
        }
        member.setDivisionId(divisionId);
        member.setActiveMemberSlot(buildActiveMemberSlot(divisionId, memberId));
        // 不再写入全局 platform slot，避免跨 Division 唯一冲突
        member.setActivePlatformSlot(null);
    }

    public void clearActiveMemberSlot(BasketTeamMember member) {
        if (member == null || member.getId() == null) {
            return;
        }
        teamMemberDao.update(null, new LambdaUpdateWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getId, member.getId())
                .set(BasketTeamMember::getActiveMemberSlot, null)
                .set(BasketTeamMember::getActivePlatformSlot, null));
    }

    public void insertMember(BasketTeamMember member) {
        try {
            teamMemberDao.insert(member);
        } catch (DuplicateKeyException ex) {
            throw new ServiceException("当前账号已在该赛事加入球队，不能重复加入");
        }
    }

    public Integer findPendingApplicationStatus(Long memberId) {
        Long count = joinApplicationDao.selectCount(new LambdaQueryWrapper<BasketTeamJoinApplication>()
                .eq(BasketTeamJoinApplication::getApplicantMemberId, memberId)
                .eq(BasketTeamJoinApplication::getApplicationStatus, TeamJoinApplicationStatusEnum.PENDING.getCode())
                .eq(BasketTeamJoinApplication::getIsDelete, NOT_DELETED));
        return count != null && count > 0 ? TeamJoinApplicationStatusEnum.PENDING.getCode() : null;
    }

    public boolean hasPendingApplication(Long memberId, Long excludeTeamId) {
        LambdaQueryWrapper<BasketTeamJoinApplication> wrapper = new LambdaQueryWrapper<BasketTeamJoinApplication>()
                .eq(BasketTeamJoinApplication::getApplicantMemberId, memberId)
                .eq(BasketTeamJoinApplication::getApplicationStatus, TeamJoinApplicationStatusEnum.PENDING.getCode())
                .eq(BasketTeamJoinApplication::getIsDelete, NOT_DELETED);
        if (excludeTeamId != null) {
            wrapper.ne(BasketTeamJoinApplication::getTeamId, excludeTeamId);
        }
        Long count = joinApplicationDao.selectCount(wrapper);
        return count != null && count > 0;
    }
}
