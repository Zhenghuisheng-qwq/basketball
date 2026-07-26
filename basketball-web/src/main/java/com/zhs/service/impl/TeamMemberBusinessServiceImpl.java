package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketFileAssetDao;
import com.zhs.dao.BasketMemberProfileDao;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.BasketTeamDao;
import com.zhs.dao.BasketTeamInvitationDao;
import com.zhs.dao.BasketTeamMemberDao;
import com.zhs.dao.BasketTournamentRegistrationDao;
import com.zhs.dao.BasketTournamentRegistrationPlayerDao;
import com.zhs.enums.PlayerStatusEnum;
import com.zhs.enums.TeamInvitationStatusEnum;
import com.zhs.enums.TeamMemberRoleEnum;
import com.zhs.enums.TeamMembershipStatusEnum;
import com.zhs.enums.TeamRosterTypeEnum;
import com.zhs.enums.TournamentRegistrationStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketFileAsset;
import com.zhs.model.BasketMemberProfile;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamInvitation;
import com.zhs.model.BasketTeamMember;
import com.zhs.model.BasketTournamentRegistration;
import com.zhs.model.BasketTournamentRegistrationPlayer;
import com.zhs.request.team.TeamMemberUpdateReq;
import com.zhs.response.team.CandidatePlayerItemResp;
import com.zhs.response.team.CandidatePlayerPageResp;
import com.zhs.response.team.TeamMemberItemResp;
import com.zhs.support.GameTerminalSupport;
import com.zhs.service.ICaptainPermissionService;
import com.zhs.service.ITeamMemberBusinessService;
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
    private BasketFileAssetDao fileAssetDao;
    @Resource
    private BasketTournamentRegistrationDao registrationDao;
    @Resource
    private BasketTournamentRegistrationPlayerDao registrationPlayerDao;
    @Resource
    private ICaptainPermissionService captainPermissionService;
    @Resource
    private TeamMemberJoinHelper teamMemberJoinHelper;
    @Resource
    private AccountSecurityLogHelper securityLogHelper;

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
        if (StringUtils.hasText(req.getJerseyNo())) {
            target.setJerseyNo(req.getJerseyNo().trim());
        }
        if (StringUtils.hasText(req.getRosterType())) {
            target.setRosterType(TeamRosterTypeEnum.fromCode(req.getRosterType()).getCode());
        }
        target.setRemark(trimToNull(req.getRemark()));
        teamMemberDao.updateById(target);
        return toMemberItem(target, loadProfileMaps(Collections.singletonList(target)));
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
        markMemberLeft(target, "被队长移除");
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
        clearPlayerCurrentTeam(membership.getPlayerId(), teamPk);
        teamMemberJoinHelper.recalculateCurrentMemberCount(teamPk);
        securityLogHelper.write(memberId, EVENT_TEAM_LEAVE, "离开球队",
                null, String.valueOf(teamPk), memberId, clientIp);
    }

    @Override
    public CandidatePlayerPageResp pageCandidatePlayers(String teamId, Integer currentPage, Integer pageSize, String keyword) {
        Long teamPk = parseLongId(teamId, "球队主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTeam team = captainPermissionService.requireTeamCaptain(memberId, teamPk);
        if (!StringUtils.hasText(team.getGameTerminal())) {
            return emptyCandidatePage(normalizePage(currentPage), normalizeSize(pageSize),
                    "当前球队比赛终端未设置，无法邀请球员");
        }

        Set<Long> existingMemberIds = teamMemberDao.selectList(new LambdaQueryWrapper<BasketTeamMember>()
                        .eq(BasketTeamMember::getTeamId, teamPk)
                        .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                        .eq(BasketTeamMember::getMembershipStatus, TeamMembershipStatusEnum.ACTIVE.getCode())
                        .eq(BasketTeamMember::getIsDelete, NOT_DELETED))
                .stream().map(BasketTeamMember::getMemberId).collect(Collectors.toSet());
        Set<Long> pendingInviteeIds = loadPendingInviteeIds(teamPk);

        LambdaQueryWrapper<BasketPlayerProfile> wrapper = new LambdaQueryWrapper<BasketPlayerProfile>()
                .eq(BasketPlayerProfile::getIsDelete, NOT_DELETED)
                .eq(BasketPlayerProfile::getCertificationStatus, 3)
                .orderByDesc(BasketPlayerProfile::getCreateTime);
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(BasketPlayerProfile::getGameNickname, kw)
                    .or().like(BasketPlayerProfile::getPlayerNo, kw));
        }

        List<BasketPlayerProfile> allProfiles = playerProfileDao.selectList(wrapper).stream()
                .filter(profile -> !existingMemberIds.contains(profile.getMemberId()))
                .filter(profile -> profile.getCurrentTeamId() == null || profile.getCurrentTeamId().equals(0L))
                .filter(profile -> !PlayerStatusEnum.fromCode(profile.getPlayerStatus()).isRestricted())
                .filter(profile -> !captainPermissionService.hasValidCaptainQualification(profile.getMemberId()))
                .filter(profile -> !pendingInviteeIds.contains(profile.getMemberId()))
                .filter(profile -> StringUtils.hasText(profile.getGameTerminal()))
                .filter(profile -> GameTerminalSupport.matches(team.getGameTerminal(), profile.getGameTerminal()))
                .collect(Collectors.toList());

        Map<Long, BasketMemberProfile> memberProfileMap = loadMemberProfiles(allProfiles.stream()
                .map(BasketPlayerProfile::getMemberId).collect(Collectors.toSet()));

        List<CandidatePlayerItemResp> items = new ArrayList<>();
        for (BasketPlayerProfile profile : allProfiles) {
            items.add(toCandidateItem(profile, memberProfileMap.get(profile.getMemberId()), team));
        }
        CandidatePlayerPageResp respPage = toCandidatePage(items, currentPage, pageSize);
        if (items.isEmpty()) {
            respPage.setEmptyReason("当前暂无与球队比赛终端一致的可邀请球员");
        }
        return respPage;
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
        List<Integer> activeStatuses = Arrays.asList(
                TournamentRegistrationStatusEnum.SUBMITTED.getCode(),
                TournamentRegistrationStatusEnum.REVIEWING.getCode(),
                TournamentRegistrationStatusEnum.APPROVED.getCode());
        List<BasketTournamentRegistration> registrations = registrationDao.selectList(new LambdaQueryWrapper<BasketTournamentRegistration>()
                .eq(BasketTournamentRegistration::getTeamId, member.getTeamId())
                .in(BasketTournamentRegistration::getRegistrationStatus, activeStatuses)
                .eq(BasketTournamentRegistration::getIsDelete, NOT_DELETED));
        if (CollectionUtils.isEmpty(registrations)) {
            return;
        }
        Set<Long> registrationIds = registrations.stream().map(BasketTournamentRegistration::getId).collect(Collectors.toSet());
        Long count = registrationPlayerDao.selectCount(new LambdaQueryWrapper<BasketTournamentRegistrationPlayer>()
                .in(BasketTournamentRegistrationPlayer::getRegistrationId, registrationIds)
                .eq(BasketTournamentRegistrationPlayer::getTeamMemberId, member.getId())
                .eq(BasketTournamentRegistrationPlayer::getIsDelete, NOT_DELETED));
        if (count != null && count > 0) {
            throw new ServiceException("该成员存在进行中的赛事报名，无法离队或移除");
        }
    }

    private void markMemberLeft(BasketTeamMember member, String reason) {
        Date now = new Date();
        member.setMembershipStatus(TeamMembershipStatusEnum.LEFT.getCode());
        member.setIsCurrent(0);
        member.setLeaveTime(now);
        member.setLeaveReason(reason);
        teamMemberDao.updateById(member);
    }

    private void clearPlayerCurrentTeam(Long playerId, Long teamId) {
        BasketPlayerProfile profile = playerProfileDao.selectById(playerId);
        if (profile != null && teamId.equals(profile.getCurrentTeamId())) {
            profile.setCurrentTeamId(null);
            playerProfileDao.updateById(profile);
        }
    }

    private BasketTeamMember requireTeamMemberRecord(Long teamMemberId, Long teamId) {
        BasketTeamMember member = teamMemberDao.selectById(teamMemberId);
        if (member == null || !NOT_DELETED.equals(member.getIsDelete()) || !teamId.equals(member.getTeamId())) {
            throw new ServiceException("球队成员不存在");
        }
        if (!Integer.valueOf(IS_CURRENT).equals(member.getIsCurrent())
                || !Objects.equals(TeamMembershipStatusEnum.ACTIVE.getCode(), member.getMembershipStatus())) {
            throw new ServiceException("球队成员状态不可用");
        }
        return member;
    }

    private List<TeamMemberItemResp> toMemberItems(List<BasketTeamMember> members) {
        if (CollectionUtils.isEmpty(members)) {
            return Collections.emptyList();
        }
        ProfileMaps maps = loadProfileMaps(members);
        List<TeamMemberItemResp> items = new ArrayList<>(members.size());
        for (BasketTeamMember member : members) {
            items.add(toMemberItem(member, maps));
        }
        return items;
    }

    private TeamMemberItemResp toMemberItem(BasketTeamMember member, ProfileMaps maps) {
        TeamMemberItemResp resp = new TeamMemberItemResp();
        resp.setTeamMemberId(toStringId(member.getId()));
        resp.setMemberId(toStringId(member.getMemberId()));
        resp.setUserId(toStringId(member.getMemberId()));
        resp.setPlayerId(toStringId(member.getPlayerId()));
        resp.setRoleType(member.getRoleType());
        resp.setRosterType(member.getRosterType());
        resp.setJerseyNo(member.getJerseyNo());
        resp.setMembershipStatus(member.getMembershipStatus());
        resp.setRemark(member.getRemark());
        resp.setJoinTime(member.getJoinTime());
        BasketPlayerProfile player = maps.playerMap().get(member.getPlayerId());
        if (player != null) {
            resp.setGameNickname(player.getGameNickname());
            resp.setPlayerNo(player.getPlayerNo());
            resp.setPositionCode(player.getPositionCode());
            resp.setPositionName(PlayerPositionSupport.resolveName(player.getPositionCode()));
            resp.setCertificationStatus(player.getCertificationStatus());
        }
        BasketMemberProfile memberProfile = maps.memberProfileMap().get(member.getMemberId());
        if (memberProfile != null) {
            resp.setNickname(memberProfile.getNickname());
            resp.setAvatarUrl(resolveAvatarUrl(memberProfile.getAvatarFileId()));
        }
        return resp;
    }

    private String resolveAvatarUrl(Long avatarFileId) {
        if (avatarFileId == null) {
            return null;
        }
        BasketFileAsset asset = fileAssetDao.selectById(avatarFileId);
        if (asset == null || !NOT_DELETED.equals(asset.getIsDelete())) {
            return null;
        }
        return asset.getAccessUrl();
    }

    private CandidatePlayerItemResp toCandidateItem(BasketPlayerProfile profile,
                                                    BasketMemberProfile memberProfile,
                                                    BasketTeam team) {
        CandidatePlayerItemResp resp = new CandidatePlayerItemResp();
        resp.setMemberId(toStringId(profile.getMemberId()));
        resp.setUserId(toStringId(profile.getMemberId()));
        resp.setPlayerId(toStringId(profile.getId()));
        resp.setPlayerNo(profile.getPlayerNo());
        resp.setGameNickname(profile.getGameNickname());
        resp.setGameTerminal(profile.getGameTerminal());
        resp.setGameAccount(profile.getGameAccount());
        resp.setCurrentTeamId(toStringId(profile.getCurrentTeamId()));
        resp.setPositionCode(profile.getPositionCode());
        resp.setPositionName(PlayerPositionSupport.resolveName(profile.getPositionCode()));
        resp.setCertificationStatus(profile.getCertificationStatus());
        if (memberProfile != null) {
            resp.setNickname(memberProfile.getNickname());
            resp.setAvatar(resolveAvatarUrl(memberProfile.getAvatarFileId()));
        }
        String cannotInviteReason = GameTerminalSupport.buildInviteMismatchMessage(
                profile.getGameTerminal(), team.getGameTerminal());
        resp.setCanInvite(cannotInviteReason == null);
        resp.setCannotInviteReason(cannotInviteReason);
        return resp;
    }

    private ProfileMaps loadProfileMaps(List<BasketTeamMember> members) {
        Set<Long> playerIds = members.stream().map(BasketTeamMember::getPlayerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> memberIds = members.stream().map(BasketTeamMember::getMemberId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, BasketPlayerProfile> playerMap = new HashMap<>();
        if (!playerIds.isEmpty()) {
            for (BasketPlayerProfile profile : playerProfileDao.selectBatchIds(playerIds)) {
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

    private record ProfileMaps(Map<Long, BasketPlayerProfile> playerMap, Map<Long, BasketMemberProfile> memberProfileMap) {
    }
}
