package com.zhs.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.dao.BasketPlayerPlatformAccountDao;
import com.zhs.dao.BasketPlayerPlatformChangeRequestDao;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.BasketTeamDao;
import com.zhs.dao.BasketTeamJoinApplicationDao;
import com.zhs.dao.BasketTeamMemberDao;
import com.zhs.enums.PlatformAccountStatusEnum;
import com.zhs.enums.PlatformChangeStatusEnum;
import com.zhs.enums.PlatformIdentityStatusEnum;
import com.zhs.enums.TeamJoinApplicationStatusEnum;
import com.zhs.enums.TeamMembershipStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketPlayerPlatformAccount;
import com.zhs.model.BasketPlayerPlatformChangeRequest;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamJoinApplication;
import com.zhs.model.BasketTeamMember;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 按 userId + platformCode 维度的球队成员与平台游戏身份校验。
 */
@Component
public class PlatformTeamMembershipSupport {

    public static final List<String> STANDARD_PLATFORMS = List.of("PC", "PS", "XBOX");

    private static final Integer NOT_DELETED = 0;
    private static final int IS_CURRENT = 1;

    @Resource
    private BasketTeamMemberDao teamMemberDao;
    @Resource
    private BasketTeamDao teamDao;
    @Resource
    private BasketPlayerProfileDao playerProfileDao;
    @Resource
    private BasketPlayerPlatformAccountDao platformAccountDao;
    @Resource
    private BasketPlayerPlatformChangeRequestDao platformChangeRequestDao;
    @Resource
    private BasketTeamJoinApplicationDao joinApplicationDao;

    public String requirePlatformCode(String platformCode) {
        String normalized = normalizePlatformCode(platformCode);
        if (normalized == null || !STANDARD_PLATFORMS.contains(normalized)) {
            throw new ServiceException("平台代码无效，仅支持 PC、PS、Xbox");
        }
        return normalized;
    }

    public String normalizePlatformCode(String platformCode) {
        return GameTerminalSupport.normalize(platformCode);
    }

    public String resolveTeamPlatformCode(BasketTeam team) {
        if (team == null || !StringUtils.hasText(team.getGameTerminal())) {
            throw new ServiceException("球队平台未设置，无法操作");
        }
        String platformCode = normalizePlatformCode(team.getGameTerminal());
        if (platformCode == null) {
            throw new ServiceException("球队平台代码无效");
        }
        return platformCode;
    }

    public BasketTeamMember findActiveMembership(Long memberId, String platformCode) {
        String normalizedPlatform = requirePlatformCode(platformCode);
        List<BasketTeamMember> memberships = teamMemberDao.selectList(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getMemberId, memberId)
                .eq(BasketTeamMember::getPlatformCode, normalizedPlatform)
                .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                .eq(BasketTeamMember::getMembershipStatus, TeamMembershipStatusEnum.ACTIVE.getCode())
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketTeamMember::getJoinTime)
                .orderByDesc(BasketTeamMember::getId));
        if (!CollectionUtils.isEmpty(memberships)) {
            return memberships.get(0);
        }
        return findActiveMembershipByTeamTerminal(memberId, normalizedPlatform);
    }

    public boolean hasActiveTeamMembership(Long memberId, String platformCode) {
        return findActiveMembership(memberId, platformCode) != null;
    }

    public String resolveJoinIneligibilityReason(Long memberId, String platformCode) {
        BasketTeamMember membership = findActiveMembership(memberId, platformCode);
        if (membership != null) {
            BasketTeam team = teamDao.selectById(membership.getTeamId());
            String teamName = team == null ? "其他球队" : team.getTeamName();
            return "当前账号在" + GameTerminalSupport.formatDisplayName(platformCode)
                    + "平台已加入球队「" + teamName + "」，不能重复加入";
        }
        return null;
    }

    public void assertEligibleToJoinTeam(Long memberId, String platformCode) {
        String reason = resolveJoinIneligibilityReason(memberId, platformCode);
        if (reason != null) {
            throw new ServiceException(reason, 403);
        }
    }

    public void assertNoActiveMembershipOnPlatform(Long memberId, String platformCode) {
        assertEligibleToJoinTeam(memberId, platformCode);
    }

    public BasketPlayerPlatformAccount requireActivePlatformIdentity(Long memberId, String platformCode) {
        String normalizedPlatform = requirePlatformCode(platformCode);
        BasketPlayerProfile profile = requirePlayerProfile(memberId);
        BasketPlayerPlatformAccount account = platformAccountDao.selectOne(new LambdaQueryWrapper<BasketPlayerPlatformAccount>()
                .eq(BasketPlayerPlatformAccount::getPlayerId, profile.getId())
                .eq(BasketPlayerPlatformAccount::getPlatform, normalizedPlatform)
                .eq(BasketPlayerPlatformAccount::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketPlayerPlatformAccount::getCreateTime)
                .last("limit 1"));
        if (account == null) {
            throw new ServiceException("请先在" + GameTerminalSupport.formatDisplayName(normalizedPlatform)
                    + "平台绑定并通过游戏 ID 审核");
        }
        if (!Objects.equals(PlatformAccountStatusEnum.ACTIVE.getCode(), account.getAccountStatus())) {
            PlatformIdentityStatusEnum status = resolvePlatformIdentityStatus(memberId, normalizedPlatform);
            if (status == PlatformIdentityStatusEnum.PENDING) {
                throw new ServiceException("该平台游戏 ID 审核中，请等待审核通过后再操作");
            }
            if (status == PlatformIdentityStatusEnum.REJECTED) {
                throw new ServiceException("该平台游戏 ID 已被驳回，请重新绑定并通过审核");
            }
            throw new ServiceException("请先在" + GameTerminalSupport.formatDisplayName(normalizedPlatform)
                    + "平台绑定并通过游戏 ID 审核");
        }
        return account;
    }

    public PlatformIdentityStatusEnum resolvePlatformIdentityStatus(Long memberId, String platformCode) {
        String normalizedPlatform = requirePlatformCode(platformCode);
        BasketPlayerProfile profile = playerProfileDao.selectOne(new LambdaQueryWrapper<BasketPlayerProfile>()
                .eq(BasketPlayerProfile::getMemberId, memberId)
                .eq(BasketPlayerProfile::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        if (profile == null) {
            return PlatformIdentityStatusEnum.UNBOUND;
        }
        if (hasPendingPlatformChange(profile.getId(), normalizedPlatform)) {
            return PlatformIdentityStatusEnum.PENDING;
        }
        BasketPlayerPlatformAccount account = platformAccountDao.selectOne(new LambdaQueryWrapper<BasketPlayerPlatformAccount>()
                .eq(BasketPlayerPlatformAccount::getPlayerId, profile.getId())
                .eq(BasketPlayerPlatformAccount::getPlatform, normalizedPlatform)
                .eq(BasketPlayerPlatformAccount::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketPlayerPlatformAccount::getCreateTime)
                .last("limit 1"));
        if (account == null) {
            return PlatformIdentityStatusEnum.UNBOUND;
        }
        if (Objects.equals(PlatformAccountStatusEnum.ACTIVE.getCode(), account.getAccountStatus())) {
            return PlatformIdentityStatusEnum.ACTIVE;
        }
        if (Objects.equals(PlatformAccountStatusEnum.REJECTED.getCode(), account.getAccountStatus())) {
            return PlatformIdentityStatusEnum.REJECTED;
        }
        return PlatformIdentityStatusEnum.PENDING;
    }

    public Integer findPendingApplicationStatus(Long memberId, String platformCode) {
        String normalizedPlatform = requirePlatformCode(platformCode);
        List<BasketTeamJoinApplication> pendingApps = joinApplicationDao.selectList(new LambdaQueryWrapper<BasketTeamJoinApplication>()
                .eq(BasketTeamJoinApplication::getApplicantMemberId, memberId)
                .eq(BasketTeamJoinApplication::getApplicationStatus, TeamJoinApplicationStatusEnum.PENDING.getCode())
                .eq(BasketTeamJoinApplication::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketTeamJoinApplication::getCreateTime));
        if (CollectionUtils.isEmpty(pendingApps)) {
            return null;
        }
        for (BasketTeamJoinApplication application : pendingApps) {
            BasketTeam team = teamDao.selectById(application.getTeamId());
            if (team != null && normalizedPlatform.equals(normalizePlatformCode(team.getGameTerminal()))) {
                return TeamJoinApplicationStatusEnum.PENDING.getCode();
            }
        }
        return null;
    }

    public boolean hasPendingApplicationOnPlatform(Long memberId, String platformCode, Long excludeTeamId) {
        String normalizedPlatform = requirePlatformCode(platformCode);
        List<BasketTeamJoinApplication> pendingApps = joinApplicationDao.selectList(new LambdaQueryWrapper<BasketTeamJoinApplication>()
                .eq(BasketTeamJoinApplication::getApplicantMemberId, memberId)
                .eq(BasketTeamJoinApplication::getApplicationStatus, TeamJoinApplicationStatusEnum.PENDING.getCode())
                .eq(BasketTeamJoinApplication::getIsDelete, NOT_DELETED));
        if (CollectionUtils.isEmpty(pendingApps)) {
            return false;
        }
        for (BasketTeamJoinApplication application : pendingApps) {
            if (excludeTeamId != null && excludeTeamId.equals(application.getTeamId())) {
                continue;
            }
            BasketTeam team = teamDao.selectById(application.getTeamId());
            if (team != null && normalizedPlatform.equals(normalizePlatformCode(team.getGameTerminal()))) {
                return true;
            }
        }
        return false;
    }

    public void assertPlatformIdentityMatchesTeam(BasketPlayerPlatformAccount account, BasketTeam team) {
        String teamPlatform = resolveTeamPlatformCode(team);
        String accountPlatform = normalizePlatformCode(account.getPlatform());
        if (!teamPlatform.equals(accountPlatform)) {
            throw new ServiceException("平台游戏身份与球队平台不一致，无法加入");
        }
    }

    public String buildActivePlatformSlot(Long memberId, String platformCode) {
        return memberId + ":" + requirePlatformCode(platformCode);
    }

    public Set<Long> loadMemberIdsWithActiveMembershipOnPlatform(String platformCode) {
        String normalizedPlatform = requirePlatformCode(platformCode);
        return teamMemberDao.selectList(new LambdaQueryWrapper<BasketTeamMember>()
                        .eq(BasketTeamMember::getPlatformCode, normalizedPlatform)
                        .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                        .eq(BasketTeamMember::getMembershipStatus, TeamMembershipStatusEnum.ACTIVE.getCode())
                        .eq(BasketTeamMember::getIsDelete, NOT_DELETED))
                .stream()
                .map(BasketTeamMember::getMemberId)
                .collect(java.util.stream.Collectors.toSet());
    }

    private BasketTeamMember findActiveMembershipByTeamTerminal(Long memberId, String platformCode) {
        List<BasketTeamMember> memberships = teamMemberDao.selectList(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getMemberId, memberId)
                .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                .eq(BasketTeamMember::getMembershipStatus, TeamMembershipStatusEnum.ACTIVE.getCode())
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED));
        if (CollectionUtils.isEmpty(memberships)) {
            return null;
        }
        for (BasketTeamMember membership : memberships) {
            if (platformCode.equals(normalizePlatformCode(membership.getPlatformCode()))) {
                return membership;
            }
            BasketTeam team = teamDao.selectById(membership.getTeamId());
            if (team != null && platformCode.equals(normalizePlatformCode(team.getGameTerminal()))) {
                return membership;
            }
        }
        return null;
    }

    private boolean hasPendingPlatformChange(Long playerId, String platformCode) {
        Long count = platformChangeRequestDao.selectCount(new LambdaQueryWrapper<BasketPlayerPlatformChangeRequest>()
                .eq(BasketPlayerPlatformChangeRequest::getPlayerId, playerId)
                .eq(BasketPlayerPlatformChangeRequest::getPlatform, platformCode)
                .eq(BasketPlayerPlatformChangeRequest::getRequestStatus, PlatformChangeStatusEnum.PENDING.getCode())
                .eq(BasketPlayerPlatformChangeRequest::getIsDelete, NOT_DELETED));
        return count != null && count > 0;
    }

    private BasketPlayerProfile requirePlayerProfile(Long memberId) {
        BasketPlayerProfile profile = playerProfileDao.selectOne(new LambdaQueryWrapper<BasketPlayerProfile>()
                .eq(BasketPlayerProfile::getMemberId, memberId)
                .eq(BasketPlayerProfile::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        if (profile == null) {
            throw new ServiceException("请先完成球员认证");
        }
        return profile;
    }
}
