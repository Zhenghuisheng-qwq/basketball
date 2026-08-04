package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.common.UserContext;
import com.zhs.dao.*;
import com.zhs.enums.PlayerStatusEnum;
import com.zhs.enums.TeamInvitationStatusEnum;
import com.zhs.enums.TeamMemberRoleEnum;
import com.zhs.enums.TeamMembershipStatusEnum;
import com.zhs.enums.TeamRosterTypeEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketFileAsset;
import com.zhs.model.BasketMemberAccount;
import com.zhs.model.BasketMemberProfile;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamInvitation;
import com.zhs.model.BasketTeamMember;
import com.zhs.request.team.TeamMemberUpdateReq;
import com.zhs.response.team.CandidatePlayerItemResp;
import com.zhs.response.team.CandidatePlayerPageResp;
import com.zhs.response.team.TeamMemberDetailResp;
import com.zhs.response.team.TeamMemberItemResp;
import com.zhs.enums.TeamJoinSourceEnum;
import com.zhs.enums.PlayerCertStatusEnum;
import com.zhs.enums.PlatformAccountStatusEnum;
import com.zhs.enums.PlatformIdentityStatusEnum;
import com.zhs.model.BasketPlayerPlatformAccount;
import com.zhs.support.PlatformTeamMembershipSupport;
import com.zhs.support.TeamMembershipSupport;
import com.zhs.service.ICaptainPermissionService;
import com.zhs.service.ITeamMemberBusinessService;
import com.zhs.service.support.BasketPublicSupport;
import com.zhs.service.team.TeamMemberJoinHelper;
import com.zhs.support.PlayerPositionSupport;
import com.zhs.util.AccountSecurityLogHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TeamMemberBusinessServiceImpl implements ITeamMemberBusinessService {

    private static final Integer NOT_DELETED = 0;
    private static final int IS_CURRENT = 1;
    private static final String EVENT_TEAM_LEAVE = "TEAM_LEAVE";
    private static final String EVENT_TEAM_REMOVE = "TEAM_REMOVE";

    @Resource
    private BasketTeamMemberDao teamMemberDao;
    @Resource
    private BasketTeamDao teamDao;
    @Resource
    private BasketTeamInvitationDao teamInvitationDao;
    @Resource
    private BasketPlayerProfileDao playerProfileDao;
    @Resource
    private BasketMemberProfileDao memberProfileDao;
    @Resource
    private BasketMemberAccountDao memberAccountDao;
    @Resource
    private BasketFileAssetDao fileAssetDao;
    @Resource
    private ICaptainPermissionService captainPermissionService;
    @Resource
    private TeamMemberJoinHelper teamMemberJoinHelper;
    @Resource
    private AccountSecurityLogHelper securityLogHelper;
    @Resource
    private BasketPlayerPlatformAccountDao platformAccountDao;
    @Resource
    private PlatformTeamMembershipSupport platformTeamMembershipSupport;
    @Resource
    private TeamMembershipSupport teamMembershipSupport;
    @Resource
    private BasketPublicSupport publicSupport;

    @Override
    public IPage<TeamMemberItemResp> pageMembers(String teamId, Integer currentPage, Integer pageSize) {
        Long teamPk = parseLongId(teamId, "球队主键");
        Long memberId = UserContext.getRequiredUserId();
        captainPermissionService.requireTeamMember(memberId, teamPk);

        Page<BasketTeamMember> page = new Page<>(normalizePage(currentPage), normalizeSize(pageSize));
        IPage<BasketTeamMember> memberPage = teamMemberDao.selectPage(page, new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getTeamId, teamPk)
                .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                .eq(BasketTeamMember::getMembershipStatus, TeamMembershipStatusEnum.ACTIVE.getCode())
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED)
                .orderByAsc(BasketTeamMember::getRoleType)
                .orderByAsc(BasketTeamMember::getJoinTime));

        Page<TeamMemberItemResp> respPage = new Page<>(memberPage.getCurrent(), memberPage.getSize(), memberPage.getTotal());
        respPage.setRecords(toMemberItems(memberPage.getRecords()));
        return respPage;
    }

    @Override
    public TeamMemberDetailResp getMemberDetail(String teamId, String teamMemberId) {
        Long teamPk = parseLongId(teamId, "球队主键");
        Long teamMemberPk = parseLongId(teamMemberId, "球队成员主键");
        Long viewerId = UserContext.getRequiredUserId();

        BasketTeamMember viewerMembership = captainPermissionService.requireTeamMember(viewerId, teamPk);
        BasketTeamMember target = requireTeamMemberRecord(teamMemberPk, teamPk);
        boolean viewerIsCaptain = TeamMemberRoleEnum.CAPTAIN.getCode().equals(viewerMembership.getRoleType());

        BasketTeam team = teamDao.selectById(teamPk);
        if (team == null || !NOT_DELETED.equals(team.getIsDelete())) {
            throw new ServiceException("球队不存在", 404);
        }

        ProfileMaps maps = loadProfileMaps(Collections.singletonList(target));
        List<BasketPlayerPlatformAccount> accounts = null;
        if (target.getPlayerId() != null) {
            BasketPlayerProfile profile = maps.playerMap().get(target.getPlayerId());
            if (profile != null) {
                accounts = loadAllPlatformAccounts(Collections.singletonList(profile)).get(target.getPlayerId());
            }
        }
        TeamMemberItemResp item = toMemberItem(target, maps, accounts, loadPlayerAvatarMap(maps));

        TeamMemberDetailResp detail = new TeamMemberDetailResp();
        detail.setTeamMemberId(item.getTeamMemberId());
        detail.setMemberId(item.getMemberId());
        detail.setUserId(item.getUserId());
        detail.setPlayerId(item.getPlayerId());
        detail.setPlayerNo(item.getPlayerNo());
        detail.setNickname(firstNonBlank(item.getNickname(), item.getGameNickname(), "未设置昵称"));
        detail.setAvatarUrl(item.getAvatarUrl());
        detail.setPositionCode(item.getPositionCode());
        detail.setPositionName(item.getPositionName());
        detail.setRoleInTeam(item.getRoleType());
        detail.setRoleInTeamText(resolveRoleText(item.getRoleType()));
        detail.setMemberStatus(item.getMembershipStatus());
        detail.setMemberStatusText(item.getMembershipStatusText());
        detail.setRosterType(item.getRosterType());
        detail.setRosterTypeText(item.getRosterTypeText());
        detail.setJoinTime(item.getJoinTime());
        detail.setJerseyNo(item.getJerseyNo());
        detail.setPlatformGameIds(item.getPlatformGameIds() == null ? Collections.emptyList() : item.getPlatformGameIds());
        detail.setGameIdSummary(item.getGameIdSummary());
        detail.setTeamId(toStringId(team.getId()));
        detail.setTeamName(team.getTeamName());
        detail.setIsCaptain(TeamMemberRoleEnum.CAPTAIN.getCode().equals(target.getRoleType()));
        detail.setCanManage(viewerIsCaptain);
        detail.setCurrentRosterStatus(firstNonBlank(item.getRosterTypeText(), item.getMembershipStatusText(), "-"));
        detail.setSourceType(target.getJoinSource());
        detail.setSourceTypeText(resolveJoinSourceText(target.getJoinSource()));
        // 参赛/数据摘要本轮不做复杂统计，前端展示「暂无数据」
        detail.setRecentMatches(Collections.emptyList());
        detail.setAuditRecords(Collections.emptyList());
        detail.setPunishmentRecords(Collections.emptyList());
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamMemberItemResp updateMember(String teamId, String teamMemberId, TeamMemberUpdateReq req) {
        if (req == null) {
            throw new ServiceException("请求参数不能为空");
        }
        Long teamPk = parseLongId(teamId, "球队主键");
        Long teamMemberPk = parseLongId(teamMemberId, "球队成员主键");
        Long memberId = UserContext.getRequiredUserId();
        captainPermissionService.requireTeamCaptain(memberId, teamPk);

        BasketTeamMember target = requireTeamMemberRecord(teamMemberPk, teamPk);
        if (req.getJerseyNo() != null) {
            String normalizedJerseyNo = req.getJerseyNo().trim();
            validateJerseyNo(target, normalizedJerseyNo, teamPk);
            target.setJerseyNo(normalizedJerseyNo);
        }
        if (StringUtils.hasText(req.getRosterType())) {
            target.setRosterType(TeamRosterTypeEnum.fromCode(req.getRosterType()).getCode());
        }
        target.setRemark(trimToNull(req.getRemark()));
        teamMemberDao.updateById(target);
        return toMemberItem(target, loadProfileMaps(Collections.singletonList(target)));
    }

    private void validateJerseyNo(BasketTeamMember target, String jerseyNo, Long teamPk) {
        if (!StringUtils.hasText(jerseyNo)) {
            throw new ServiceException("球衣号不能为空");
        }
        String normalized = jerseyNo.trim();
        if (!normalized.matches("^\\d{1,3}$")) {
            throw new ServiceException("球衣号格式不正确，请输入1-3位数字");
        }
        Long duplicateCount = teamMemberDao.selectCount(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getTeamId, teamPk)
                .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                .eq(BasketTeamMember::getMembershipStatus, TeamMembershipStatusEnum.ACTIVE.getCode())
                .eq(BasketTeamMember::getJerseyNo, normalized)
                .ne(BasketTeamMember::getId, target.getId())
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED));
        if (duplicateCount != null && duplicateCount > 0) {
            throw new ServiceException("该球衣号已被队内其他成员使用");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(String teamId, String teamMemberId, String clientIp) {
        Long teamPk = parseLongId(teamId, "球队主键");
        Long teamMemberPk = parseLongId(teamMemberId, "球队成员主键");
        Long memberId = UserContext.getRequiredUserId();
        captainPermissionService.requireTeamCaptain(memberId, teamPk);

        BasketTeamMember target = requireTeamMemberRecord(teamMemberPk, teamPk);
        if (memberId.equals(target.getMemberId())) {
            throw new ServiceException("不能移除自己，请使用离队功能");
        }
        if (TeamMemberRoleEnum.CAPTAIN.getCode().equals(target.getRoleType())) {
            throw new ServiceException("不能移除队长");
        }
        assertNotInActiveRegistration(target);
        markMemberRemoved(target, "被队长移除");
        teamMemberJoinHelper.clearActiveMemberSlot(target);
        clearPlayerCurrentTeam(target.getPlayerId(), teamPk);
        teamMemberJoinHelper.recalculateCurrentMemberCount(teamPk);
        securityLogHelper.write(memberId, EVENT_TEAM_REMOVE, "移除球队成员",
                null, String.valueOf(target.getMemberId()), memberId, clientIp);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leave(String teamId, String clientIp) {
        Long teamPk = parseLongId(teamId, "球队主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTeamMember membership = captainPermissionService.requireTeamMember(memberId, teamPk);
        if (TeamMemberRoleEnum.CAPTAIN.getCode().equals(membership.getRoleType())) {
            throw new ServiceException("队长不能直接离队");
        }
        assertNotInActiveRegistration(membership);
        markMemberLeft(membership, "主动离队");
        teamMemberJoinHelper.clearActiveMemberSlot(membership);
        clearPlayerCurrentTeam(membership.getPlayerId(), teamPk);
        teamMemberJoinHelper.recalculateCurrentMemberCount(teamPk);
        securityLogHelper.write(memberId, EVENT_TEAM_LEAVE, "离开球队",
                null, String.valueOf(teamPk), memberId, clientIp);
    }

    @Override
    public CandidatePlayerPageResp pageCandidatePlayers(String teamId, Integer currentPage, Integer pageSize, String keyword) {
        Long teamPk = parseLongId(teamId, "球队主键");
        Long memberId = UserContext.getRequiredUserId();
        captainPermissionService.requireTeamCaptain(memberId, teamPk);

        Set<Long> existingMemberIds = teamMemberDao.selectList(new LambdaQueryWrapper<BasketTeamMember>()
                        .eq(BasketTeamMember::getTeamId, teamPk)
                        .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                        .eq(BasketTeamMember::getMembershipStatus, TeamMembershipStatusEnum.ACTIVE.getCode())
                        .eq(BasketTeamMember::getIsDelete, NOT_DELETED))
                .stream().map(BasketTeamMember::getMemberId).collect(Collectors.toSet());
        Set<Long> pendingInviteeIds = loadPendingInviteeIds(teamPk);

        Set<Long> keywordPlayerIds = loadKeywordMatchedPlayerIds(keyword);
        LambdaQueryWrapper<BasketPlayerProfile> wrapper = new LambdaQueryWrapper<BasketPlayerProfile>()
                .eq(BasketPlayerProfile::getIsDelete, NOT_DELETED)
                .eq(BasketPlayerProfile::getCertificationStatus, PlayerCertStatusEnum.APPROVED.getCode())
                .orderByDesc(BasketPlayerProfile::getCreateTime);
        if (!CollectionUtils.isEmpty(keywordPlayerIds)) {
            wrapper.in(BasketPlayerProfile::getId, keywordPlayerIds);
        } else if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(BasketPlayerProfile::getGameNickname, kw)
                    .or().like(BasketPlayerProfile::getPlayerNo, kw));
        }

        List<BasketPlayerProfile> allProfiles = playerProfileDao.selectList(wrapper).stream()
                .filter(profile -> !existingMemberIds.contains(profile.getMemberId()))
                .filter(profile -> !PlayerStatusEnum.fromCode(profile.getPlayerStatus()).isRestricted())
                .filter(profile -> !pendingInviteeIds.contains(profile.getMemberId()))
                .collect(Collectors.toList());

        Map<Long, BasketMemberProfile> memberProfileMap = loadMemberProfiles(allProfiles.stream()
                .map(BasketPlayerProfile::getMemberId).collect(Collectors.toSet()));
        Map<Long, List<BasketPlayerPlatformAccount>> platformAccountsMap = loadAllPlatformAccounts(allProfiles);

        List<CandidatePlayerItemResp> items = new ArrayList<>();
        for (BasketPlayerProfile profile : allProfiles) {
            items.add(toInviteCandidateItem(profile, memberProfileMap.get(profile.getMemberId()), platformAccountsMap.get(profile.getId())));
        }
        CandidatePlayerPageResp respPage = toCandidatePage(items, currentPage, pageSize);
        if (items.isEmpty()) {
            respPage.setEmptyReason("当前暂无可邀请的已认证球员");
        }
        return respPage;
    }

    private Map<Long, List<BasketPlayerPlatformAccount>> loadAllPlatformAccounts(List<BasketPlayerProfile> profiles) {
        if (CollectionUtils.isEmpty(profiles)) {
            return Collections.emptyMap();
        }
        Set<Long> playerIds = profiles.stream().map(BasketPlayerProfile::getId).collect(Collectors.toSet());
        Map<Long, List<BasketPlayerPlatformAccount>> map = new HashMap<>();
        for (BasketPlayerPlatformAccount account : platformAccountDao.selectList(new LambdaQueryWrapper<BasketPlayerPlatformAccount>()
                .in(BasketPlayerPlatformAccount::getPlayerId, playerIds)
                .eq(BasketPlayerPlatformAccount::getIsDelete, NOT_DELETED))) {
            map.computeIfAbsent(account.getPlayerId(), key -> new ArrayList<>()).add(account);
        }
        return map;
    }

    private CandidatePlayerItemResp toInviteCandidateItem(BasketPlayerProfile profile,
                                                          BasketMemberProfile memberProfile,
                                                          List<BasketPlayerPlatformAccount> platformAccounts) {
        CandidatePlayerItemResp resp = new CandidatePlayerItemResp();
        resp.setMemberId(toStringId(profile.getMemberId()));
        resp.setUserId(toStringId(profile.getMemberId()));
        resp.setPlayerId(toStringId(profile.getId()));
        resp.setPlayerNo(profile.getPlayerNo());
        resp.setGameNickname(profile.getGameNickname());
        resp.setGameAccount(profile.getGameAccount());
        resp.setPositionCode(profile.getPositionCode());
        resp.setPositionName(PlayerPositionSupport.resolveName(profile.getPositionCode()));
        resp.setCertificationStatus(profile.getCertificationStatus());
        resp.setCertificationStatusText(PlayerCertStatusEnum.APPROVED.getDescription());
        if (memberProfile != null) {
            resp.setNickname(memberProfile.getNickname());
            resp.setAvatar(resolveAvatarUrl(memberProfile.getAvatarFileId()));
        }
        resp.setPlatformAccountSummary(buildPlatformAccountSummary(platformAccounts));
        BasketTeamMember activeMembership = teamMembershipSupport.findActiveMembership(profile.getMemberId());
        if (activeMembership != null) {
            BasketTeam memberTeam = teamDao.selectById(activeMembership.getTeamId());
            resp.setCurrentTeamId(toStringId(activeMembership.getTeamId()));
            resp.setCurrentTeamStatusText(memberTeam == null ? "已加入球队" : "已加入 " + memberTeam.getTeamName());
        } else {
            resp.setCurrentTeamStatusText("未加入球队");
        }
        String cannotInviteReason = resolveInviteCannotReason(profile, activeMembership);
        resp.setCanInvite(cannotInviteReason == null);
        resp.setCannotInviteReason(cannotInviteReason);
        return resp;
    }

    private String buildPlatformAccountSummary(List<BasketPlayerPlatformAccount> platformAccounts) {
        String summary = com.zhs.support.PlatformGameIdDisplaySupport.summary(platformAccounts);
        if (com.zhs.support.PlatformGameIdDisplaySupport.UNBOUND.equals(summary)) {
            return "暂无平台游戏 ID";
        }
        return summary;
    }

    private String resolveInviteCannotReason(BasketPlayerProfile profile, BasketTeamMember activeMembership) {
        if (!Objects.equals(PlayerCertStatusEnum.APPROVED.getCode(), profile.getCertificationStatus())) {
            return "球员未认证";
        }
        if (activeMembership != null) {
            return "该球员已加入其他球队";
        }
        return null;
    }

    private Set<Long> loadKeywordMatchedPlayerIds(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptySet();
        }
        String kw = keyword.trim();
        Set<Long> playerIds = new HashSet<>();
        List<BasketPlayerPlatformAccount> platformMatches = platformAccountDao.selectList(
                new LambdaQueryWrapper<BasketPlayerPlatformAccount>()
                        .eq(BasketPlayerPlatformAccount::getIsDelete, NOT_DELETED)
                        .and(w -> w.like(BasketPlayerPlatformAccount::getDisplayGameId, kw)
                                .or().like(BasketPlayerPlatformAccount::getNormalizedGameId, kw.toLowerCase(Locale.ROOT))));
        for (BasketPlayerPlatformAccount account : platformMatches) {
            playerIds.add(account.getPlayerId());
        }
        List<BasketMemberProfile> memberMatches = memberProfileDao.selectList(new LambdaQueryWrapper<BasketMemberProfile>()
                .eq(BasketMemberProfile::getIsDelete, NOT_DELETED)
                .like(BasketMemberProfile::getNickname, kw));
        if (!CollectionUtils.isEmpty(memberMatches)) {
            Set<Long> memberIds = memberMatches.stream().map(BasketMemberProfile::getMemberId).collect(Collectors.toSet());
            appendProfilesByMemberIds(playerIds, memberIds);
        }
        List<BasketMemberAccount> accountMatches = memberAccountDao.selectList(new LambdaQueryWrapper<BasketMemberAccount>()
                .eq(BasketMemberAccount::getIsDelete, NOT_DELETED)
                .and(w -> w.like(BasketMemberAccount::getUsername, kw)
                        .or().like(BasketMemberAccount::getAccountNo, kw)));
        if (!CollectionUtils.isEmpty(accountMatches)) {
            Set<Long> memberIds = accountMatches.stream().map(BasketMemberAccount::getId).collect(Collectors.toSet());
            appendProfilesByMemberIds(playerIds, memberIds);
        }
        List<BasketPlayerProfile> playerNoMatches = playerProfileDao.selectList(new LambdaQueryWrapper<BasketPlayerProfile>()
                .eq(BasketPlayerProfile::getIsDelete, NOT_DELETED)
                .like(BasketPlayerProfile::getPlayerNo, kw));
        for (BasketPlayerProfile profile : playerNoMatches) {
            playerIds.add(profile.getId());
        }
        return playerIds;
    }

    private void appendProfilesByMemberIds(Set<Long> playerIds, Set<Long> memberIds) {
        if (CollectionUtils.isEmpty(memberIds)) {
            return;
        }
        for (BasketPlayerProfile profile : playerProfileDao.selectList(new LambdaQueryWrapper<BasketPlayerProfile>()
                .in(BasketPlayerProfile::getMemberId, memberIds)
                .eq(BasketPlayerProfile::getIsDelete, NOT_DELETED))) {
            playerIds.add(profile.getId());
        }
    }

    private Map<Long, BasketPlayerPlatformAccount> loadPlatformAccounts(List<BasketPlayerProfile> profiles,
                                                                        String platformCode) {
        if (CollectionUtils.isEmpty(profiles)) {
            return Collections.emptyMap();
        }
        Set<Long> playerIds = profiles.stream().map(BasketPlayerProfile::getId).collect(Collectors.toSet());
        Map<Long, BasketPlayerPlatformAccount> map = new HashMap<>();
        for (BasketPlayerPlatformAccount account : platformAccountDao.selectList(new LambdaQueryWrapper<BasketPlayerPlatformAccount>()
                .in(BasketPlayerPlatformAccount::getPlayerId, playerIds)
                .eq(BasketPlayerPlatformAccount::getPlatform, platformCode)
                .eq(BasketPlayerPlatformAccount::getIsDelete, NOT_DELETED))) {
            map.putIfAbsent(account.getPlayerId(), account);
        }
        return map;
    }

    private Set<Long> loadPendingInviteeIds(Long teamId) {
        return teamInvitationDao.selectList(new LambdaQueryWrapper<BasketTeamInvitation>()
                        .eq(BasketTeamInvitation::getTeamId, teamId)
                        .eq(BasketTeamInvitation::getInvitationStatus, TeamInvitationStatusEnum.PENDING.getCode())
                        .eq(BasketTeamInvitation::getIsDelete, NOT_DELETED))
                .stream()
                .map(BasketTeamInvitation::getInviteeMemberId)
                .collect(Collectors.toSet());
    }

    private CandidatePlayerPageResp emptyCandidatePage(long page, long size, String emptyReason) {
        CandidatePlayerPageResp respPage = new CandidatePlayerPageResp(page, size, 0);
        respPage.setRecords(Collections.emptyList());
        respPage.setEmptyReason(emptyReason);
        return respPage;
    }

    private CandidatePlayerPageResp toCandidatePage(List<CandidatePlayerItemResp> items, Integer currentPage, Integer pageSize) {
        long page = normalizePage(currentPage);
        long size = normalizeSize(pageSize);
        long total = items.size();
        int from = (int) Math.min((page - 1) * size, total);
        int to = (int) Math.min(from + size, total);
        List<CandidatePlayerItemResp> records = from >= to ? Collections.emptyList() : items.subList(from, to);
        CandidatePlayerPageResp respPage = new CandidatePlayerPageResp(page, size, total);
        respPage.setRecords(records);
        return respPage;
    }

    private void assertNotInActiveRegistration(BasketTeamMember member) {
        // legacy registration domain removed in R5
    }

    private void markMemberRemoved(BasketTeamMember member, String reason) {
        Date now = new Date();
        member.setMembershipStatus(TeamMembershipStatusEnum.REMOVED.getCode());
        member.setIsCurrent(0);
        member.setLeaveTime(now);
        member.setLeaveReason(reason);
        teamMemberDao.updateById(member);
        teamMemberJoinHelper.clearActiveMemberSlot(member);
    }

    private void markMemberLeft(BasketTeamMember member, String reason) {
        Date now = new Date();
        member.setMembershipStatus(TeamMembershipStatusEnum.LEFT.getCode());
        member.setIsCurrent(0);
        member.setLeaveTime(now);
        member.setLeaveReason(reason);
        teamMemberDao.updateById(member);
        // active_*_slot 由 clearActiveMemberSlot 用 UpdateWrapper 置空，避免 MP 忽略 null
    }

    private void clearPlayerCurrentTeam(Long playerId, Long teamId) {
        if (playerId == null || teamId == null) {
            return;
        }
        playerProfileDao.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<BasketPlayerProfile>()
                .eq(BasketPlayerProfile::getId, playerId)
                .eq(BasketPlayerProfile::getCurrentTeamId, teamId)
                .set(BasketPlayerProfile::getCurrentTeamId, null));
    }

    private BasketTeamMember requireTeamMemberRecord(Long teamMemberId, Long teamId) {
        BasketTeamMember member = teamMemberDao.selectById(teamMemberId);
        if (member == null || !NOT_DELETED.equals(member.getIsDelete()) || !teamId.equals(member.getTeamId())) {
            throw new ServiceException("球队成员不存在", 404);
        }
        if (!Integer.valueOf(IS_CURRENT).equals(member.getIsCurrent())
                || !Objects.equals(TeamMembershipStatusEnum.ACTIVE.getCode(), member.getMembershipStatus())) {
            throw new ServiceException("球队成员状态不可用", 404);
        }
        return member;
    }

    private String resolveRoleText(String roleType) {
        if (!StringUtils.hasText(roleType)) {
            return "-";
        }
        try {
            return TeamMemberRoleEnum.fromCode(roleType).getDescription();
        } catch (Exception ex) {
            return roleType;
        }
    }

    private String resolveJoinSourceText(String source) {
        if (!StringUtils.hasText(source)) {
            return null;
        }
        try {
            TeamJoinSourceEnum joinSource = TeamJoinSourceEnum.fromCode(source);
            if (joinSource == TeamJoinSourceEnum.APPLY) {
                return "申请加入";
            }
            if (joinSource == TeamJoinSourceEnum.INVITE) {
                return "队长邀请";
            }
            return joinSource.getDescription();
        } catch (Exception ex) {
            return source;
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private List<TeamMemberItemResp> toMemberItems(List<BasketTeamMember> members) {
        if (CollectionUtils.isEmpty(members)) {
            return Collections.emptyList();
        }
        ProfileMaps maps = loadProfileMaps(members);
        Map<Long, String> playerAvatarMap = loadPlayerAvatarMap(maps);
        List<BasketPlayerProfile> profiles = new ArrayList<>(maps.playerMap().values());
        Map<Long, List<BasketPlayerPlatformAccount>> platformAccountsMap = loadAllPlatformAccounts(profiles);
        List<TeamMemberItemResp> items = new ArrayList<>(members.size());
        for (BasketTeamMember member : members) {
            items.add(toMemberItem(member, maps, platformAccountsMap.get(member.getPlayerId()), playerAvatarMap));
        }
        return items;
    }

    private Map<Long, String> loadPlayerAvatarMap(ProfileMaps maps) {
        if (maps == null || maps.playerMap().isEmpty()) {
            return Collections.emptyMap();
        }
        return publicSupport.loadPlayerAvatarUrlMap(maps.playerMap().values());
    }

    private TeamMemberItemResp toMemberItem(BasketTeamMember member, ProfileMaps maps) {
        return toMemberItem(member, maps, null, loadPlayerAvatarMap(maps));
    }

    private TeamMemberItemResp toMemberItem(BasketTeamMember member, ProfileMaps maps,
                                            List<BasketPlayerPlatformAccount> platformAccounts) {
        return toMemberItem(member, maps, platformAccounts, loadPlayerAvatarMap(maps));
    }

    private TeamMemberItemResp toMemberItem(BasketTeamMember member, ProfileMaps maps,
                                            List<BasketPlayerPlatformAccount> platformAccounts,
                                            Map<Long, String> playerAvatarMap) {
        TeamMemberItemResp resp = new TeamMemberItemResp();
        resp.setTeamMemberId(toStringId(member.getId()));
        resp.setMemberId(toStringId(member.getMemberId()));
        resp.setUserId(toStringId(member.getMemberId()));
        resp.setPlayerId(toStringId(member.getPlayerId()));
        resp.setRoleType(member.getRoleType());
        resp.setRosterType(member.getRosterType());
        resp.setJerseyNo(member.getJerseyNo());
        resp.setMembershipStatus(member.getMembershipStatus());
        resp.setMembershipStatusText(resolveMembershipStatusText(member.getMembershipStatus(), member.getRosterType()));
        if (StringUtils.hasText(member.getRosterType())) {
            resp.setRosterTypeText(TeamRosterTypeEnum.fromCode(member.getRosterType()).getDescription());
        }
        resp.setRemark(member.getRemark());
        resp.setJoinTime(member.getJoinTime());
        BasketPlayerProfile player = maps.playerMap().get(member.getPlayerId());
        if (player != null) {
            resp.setGameNickname(player.getGameNickname());
            resp.setPlayerNo(player.getPlayerNo());
            resp.setPositionCode(player.getPositionCode());
            resp.setPositionName(PlayerPositionSupport.resolveName(player.getPositionCode()));
            resp.setCertificationStatus(player.getCertificationStatus());
            resp.setGameTerminal(player.getGameTerminal());
            resp.setGameAccount(player.getGameAccount());
            resp.setPlayerStatus(player.getPlayerStatus());
            resp.setCurrentTeamId(toStringId(player.getCurrentTeamId()));
            resp.setAvatarUrl(playerAvatarMap.get(player.getId()));
        }
        BasketMemberProfile memberProfile = maps.memberProfileMap().get(member.getMemberId());
        if (memberProfile != null) {
            resp.setNickname(memberProfile.getNickname());
            if (!StringUtils.hasText(resp.getAvatarUrl())) {
                resp.setAvatarUrl(resolveAvatarUrl(memberProfile.getAvatarFileId()));
            }
        }
        List<BasketPlayerPlatformAccount> accounts = platformAccounts;
        if (accounts == null && member.getPlayerId() != null) {
            BasketPlayerProfile profile = maps.playerMap().get(member.getPlayerId());
            if (profile != null) {
                accounts = loadAllPlatformAccounts(Collections.singletonList(profile)).get(member.getPlayerId());
            }
        }
        resp.setPlatformGameIds(com.zhs.support.PlatformGameIdDisplaySupport.toItems(accounts));
        resp.setGameIdSummary(com.zhs.support.PlatformGameIdDisplaySupport.summary(accounts));
        return resp;
    }

    private String resolveAvatarUrl(Long avatarFileId) {
        if (avatarFileId == null) {
            return com.zhs.support.MemberAvatarSupport.resolve(null);
        }
        Map<Long, String> urlMap = publicSupport.loadAccessUrlMap(Collections.singleton(avatarFileId));
        String url = urlMap.get(avatarFileId);
        return com.zhs.support.MemberAvatarSupport.resolve(url);
    }

    private CandidatePlayerItemResp toCandidateItem(BasketPlayerProfile profile,
                                                    BasketMemberProfile memberProfile,
                                                    BasketTeam team,
                                                    String platformCode,
                                                    BasketPlayerPlatformAccount platformAccount) {
        CandidatePlayerItemResp resp = new CandidatePlayerItemResp();
        resp.setMemberId(toStringId(profile.getMemberId()));
        resp.setUserId(toStringId(profile.getMemberId()));
        resp.setPlayerId(toStringId(profile.getId()));
        resp.setPlayerNo(profile.getPlayerNo());
        resp.setGameNickname(profile.getGameNickname());
        resp.setGameTerminal(team.getGameTerminal());
        resp.setPlatformCode(platformCode);
        resp.setGameAccount(profile.getGameAccount());
        resp.setPositionCode(profile.getPositionCode());
        resp.setPositionName(PlayerPositionSupport.resolveName(profile.getPositionCode()));
        resp.setCertificationStatus(profile.getCertificationStatus());
        if (memberProfile != null) {
            resp.setNickname(memberProfile.getNickname());
            resp.setAvatar(resolveAvatarUrl(memberProfile.getAvatarFileId()));
        }
        PlatformIdentityStatusEnum identityStatus = platformTeamMembershipSupport.resolvePlatformIdentityStatus(
                profile.getMemberId(), platformCode);
        if (platformAccount != null) {
            resp.setPlatformGameId(platformAccount.getDisplayGameId());
            PlatformAccountStatusEnum accountStatus = PlatformAccountStatusEnum.fromCode(platformAccount.getAccountStatus());
            if (Objects.equals(PlatformAccountStatusEnum.ACTIVE.getCode(), platformAccount.getAccountStatus())) {
                identityStatus = PlatformIdentityStatusEnum.ACTIVE;
            } else if (Objects.equals(PlatformAccountStatusEnum.REJECTED.getCode(), platformAccount.getAccountStatus())) {
                identityStatus = PlatformIdentityStatusEnum.REJECTED;
            } else {
                identityStatus = PlatformIdentityStatusEnum.PENDING;
            }
            resp.setPlatformIdentityStatus(identityStatus.getCode());
            resp.setPlatformIdentityStatusText(identityStatus.getLabel());
        } else {
            resp.setPlatformIdentityStatus(identityStatus.getCode());
            resp.setPlatformIdentityStatusText(identityStatus.getLabel());
        }
        BasketTeamMember activeMembership = platformTeamMembershipSupport.findActiveMembership(
                profile.getMemberId(), platformCode);
        if (activeMembership != null) {
            BasketTeam memberTeam = teamDao.selectById(activeMembership.getTeamId());
            resp.setPlatformTeamId(toStringId(activeMembership.getTeamId()));
            resp.setPlatformTeamName(memberTeam == null ? null : memberTeam.getTeamName());
            resp.setCurrentTeamId(toStringId(activeMembership.getTeamId()));
        }
        String cannotInviteReason = resolveCannotInviteReason(profile.getMemberId(), platformCode, identityStatus);
        resp.setCanInvite(cannotInviteReason == null);
        resp.setCannotInviteReason(cannotInviteReason);
        return resp;
    }

    private String resolveCannotInviteReason(Long memberId, String platformCode,
                                             PlatformIdentityStatusEnum identityStatus) {
        if (identityStatus != PlatformIdentityStatusEnum.ACTIVE) {
            if (identityStatus == PlatformIdentityStatusEnum.PENDING) {
                return "该平台游戏 ID 审核中";
            }
            if (identityStatus == PlatformIdentityStatusEnum.REJECTED) {
                return "该平台游戏 ID 已被驳回";
            }
            return "尚未绑定该平台已生效游戏 ID";
        }
        return platformTeamMembershipSupport.resolveJoinIneligibilityReason(memberId, platformCode);
    }

    private CandidatePlayerItemResp toCandidateItem(BasketPlayerProfile profile,
                                                    BasketMemberProfile memberProfile,
                                                    BasketTeam team) {
        return toCandidateItem(profile, memberProfile, team,
                platformTeamMembershipSupport.resolveTeamPlatformCode(team), null);
    }

    private ProfileMaps loadProfileMaps(List<BasketTeamMember> members) {
        Set<Long> playerIds = members.stream().map(BasketTeamMember::getPlayerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> memberIds = members.stream().map(BasketTeamMember::getMemberId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, BasketPlayerProfile> playerMap = new HashMap<>();
        if (!playerIds.isEmpty()) {
            for (BasketPlayerProfile profile : playerProfileDao.selectByIds(playerIds)) {
                if (profile != null && NOT_DELETED.equals(profile.getIsDelete())) {
                    playerMap.put(profile.getId(), profile);
                }
            }
        }
        return new ProfileMaps(playerMap, loadMemberProfiles(memberIds));
    }

    private Map<Long, BasketMemberProfile> loadMemberProfiles(Set<Long> memberIds) {
        if (CollectionUtils.isEmpty(memberIds)) {
            return Collections.emptyMap();
        }
        Map<Long, BasketMemberProfile> map = new HashMap<>();
        for (BasketMemberProfile profile : memberProfileDao.selectList(new LambdaQueryWrapper<BasketMemberProfile>()
                .in(BasketMemberProfile::getMemberId, memberIds)
                .eq(BasketMemberProfile::getIsDelete, NOT_DELETED))) {
            map.put(profile.getMemberId(), profile);
        }
        return map;
    }

    private <T> IPage<T> toPage(List<T> items, Integer currentPage, Integer pageSize) {
        long page = normalizePage(currentPage);
        long size = normalizeSize(pageSize);
        long total = items.size();
        int from = (int) Math.min((page - 1) * size, total);
        int to = (int) Math.min(from + size, total);
        List<T> records = from >= to ? Collections.emptyList() : items.subList(from, to);
        Page<T> respPage = new Page<>(page, size, total);
        respPage.setRecords(records);
        return respPage;
    }

    private long normalizePage(Integer currentPage) {
        return currentPage == null || currentPage < 1 ? 1L : currentPage;
    }

    private long normalizeSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10L;
        }
        return Math.min(pageSize, 100L);
    }

    private Long parseLongId(String id, String fieldName) {
        if (!StringUtils.hasText(id)) {
            throw new ServiceException(fieldName + "不能为空");
        }
        try {
            return Long.parseLong(id.trim());
        } catch (NumberFormatException ex) {
            throw new ServiceException(fieldName + "格式不正确");
        }
    }

    private String toStringId(Long id) {
        return id == null ? null : String.valueOf(id);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String resolveMembershipStatusText(Integer membershipStatus, String rosterType) {
        if (membershipStatus == null) {
            return null;
        }
        if (TeamMembershipStatusEnum.LEFT.getCode() == membershipStatus) {
            return TeamMembershipStatusEnum.LEFT.getDescription();
        }
        if (TeamMembershipStatusEnum.REMOVED.getCode() == membershipStatus) {
            return TeamMembershipStatusEnum.REMOVED.getDescription();
        }
        if (TeamMembershipStatusEnum.ACTIVE.getCode() == membershipStatus && StringUtils.hasText(rosterType)) {
            return TeamRosterTypeEnum.fromCode(rosterType).getDescription();
        }
        return TeamMembershipStatusEnum.ACTIVE.getDescription();
    }

    private record ProfileMaps(Map<Long, BasketPlayerProfile> playerMap, Map<Long, BasketMemberProfile> memberProfileMap) {
    }
}
