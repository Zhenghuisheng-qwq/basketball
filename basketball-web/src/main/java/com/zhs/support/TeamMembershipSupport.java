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

import java.util.List;

/**
 * 球队本体成员关系校验：一个用户当前最多加入一支有效球队，不按平台区分。
 */
@Component
public class TeamMembershipSupport {

    private static final Integer NOT_DELETED = 0;
    private static final int IS_CURRENT = 1;

    @Resource
    private BasketTeamMemberDao teamMemberDao;
    @Resource
    private BasketTeamJoinApplicationDao joinApplicationDao;

    public BasketTeamMember findActiveMembership(Long memberId) {
        List<BasketTeamMember> memberships = teamMemberDao.selectList(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getMemberId, memberId)
                .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                .eq(BasketTeamMember::getMembershipStatus, TeamMembershipStatusEnum.ACTIVE.getCode())
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketTeamMember::getJoinTime)
                .orderByDesc(BasketTeamMember::getId));
        return CollectionUtils.isEmpty(memberships) ? null : memberships.get(0);
    }

    public boolean hasActiveTeamMembership(Long memberId) {
        return findActiveMembership(memberId) != null;
    }

    public String resolveJoinIneligibilityReason(Long memberId) {
        BasketTeamMember membership = findActiveMembership(memberId);
        if (membership != null) {
            return "当前账号已加入球队，不能重复加入";
        }
        return null;
    }

    public void assertEligibleToJoinTeam(Long memberId) {
        String reason = resolveJoinIneligibilityReason(memberId);
        if (reason != null) {
            throw new ServiceException(reason, 403);
        }
    }

    public String buildActiveMemberSlot(Long memberId) {
        if (memberId == null) {
            throw new ServiceException("会员主键不能为空");
        }
        return String.valueOf(memberId);
    }

    public void applyActiveMemberSlot(BasketTeamMember member, Long memberId) {
        member.setActiveMemberSlot(buildActiveMemberSlot(memberId));
    }

    public void clearActiveMemberSlot(BasketTeamMember member) {
        if (member == null || member.getId() == null) {
            return;
        }
        // updateById 默认忽略 null，必须用 UpdateWrapper 才能释放唯一键槽位
        teamMemberDao.update(null, new LambdaUpdateWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getId, member.getId())
                .set(BasketTeamMember::getActiveMemberSlot, null)
                .set(BasketTeamMember::getActivePlatformSlot, null));
    }

    public void insertMember(BasketTeamMember member) {
        try {
            teamMemberDao.insert(member);
        } catch (DuplicateKeyException ex) {
            throw new ServiceException("当前账号已加入球队，不能重复加入");
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
