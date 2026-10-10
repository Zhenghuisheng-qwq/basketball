package com.zhs.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.common.UserContext;
import com.zhs.config.AccountTeamProperties;
import com.zhs.dao.BasketMemberProfileDao;
import com.zhs.dao.BasketTeamDao;
import com.zhs.dao.BasketFileAssetDao;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.BasketTeamInvitationDao;
import com.zhs.dao.v4.BasketDivisionDao;
import com.zhs.dao.v4.BasketSeasonDao;
import com.zhs.dao.v4.BasketSeriesDao;
import com.zhs.enums.PlatformIdentityStatusEnum;
import com.zhs.enums.TeamAuditStatusEnum;
import com.zhs.enums.TeamInvitationStatusEnum;
import com.zhs.enums.TeamStatusEnum;
import com.zhs.exception.BusinessReason;
import com.zhs.i18n.NoticeParams;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketMemberProfile;
import com.zhs.model.BasketFileAsset;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamInvitation;
import com.zhs.model.v4.BasketDivision;
import com.zhs.model.v4.BasketSeason;
import com.zhs.model.v4.BasketSeries;
import com.zhs.request.team.TeamInvitationCreateReq;
import com.zhs.request.team.TeamInvitationRespondReq;
import com.zhs.response.team.TeamInvitationItemResp;
import com.zhs.service.IBusinessMessageService;
import com.zhs.service.ICaptainPermissionService;
import com.zhs.service.ITeamInvitationBusinessService;
import com.zhs.service.team.TeamMemberJoinHelper;
import com.zhs.support.GameTerminalSupport;
import com.zhs.support.PlatformTeamMembershipSupport;
import com.zhs.support.TeamMembershipSupport;
import com.zhs.support.TeamWritableSupport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TeamInvitationBusinessServiceImpl implements ITeamInvitationBusinessService {

    private static final Integer NOT_DELETED = 0;
    private static final String BIZ_TYPE_TEAM_INVITE = "TEAM_INVITE";

    @Resource
    private BasketTeamInvitationDao invitationDao;
    @Resource
    private BasketTeamDao teamDao;
    @Resource
    private BasketMemberProfileDao memberProfileDao;
    @Resource
    private BasketFileAssetDao fileAssetDao;
    @Resource
    private ICaptainPermissionService captainPermissionService;
    @Resource
    private TeamMemberJoinHelper teamMemberJoinHelper;
    @Resource
    private IBusinessMessageService businessMessageService;
    @Resource
    private AccountTeamProperties teamProperties;
    @Resource
    private BasketPlayerProfileDao playerProfileDao;
    @Resource
    private PlatformTeamMembershipSupport platformTeamMembershipSupport;
    @Resource
    private TeamMembershipSupport teamMembershipSupport;
    @Resource
    private BasketDivisionDao divisionDao;
    @Resource
    private BasketSeasonDao seasonDao;
    @Resource
    private BasketSeriesDao seriesDao;
    @Resource
    private TeamWritableSupport teamWritableSupport;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamInvitationItemResp createInvitation(String teamId, TeamInvitationCreateReq req) {
        if (req == null || (!StringUtils.hasText(req.getInviteePlayerId()) && !StringUtils.hasText(req.getInviteeMemberId()))) {
            throw new ServiceException("被邀请球员不能为空");
        }
        Long teamPk = parseLongId(teamId, "球队主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTeam team = captainPermissionService.requireTeamCaptain(memberId, teamPk);
        teamWritableSupport.assertTeamWritable(team);

        BasketPlayerProfile inviteeProfile;
        Long inviteeMemberId;
        if (StringUtils.hasText(req.getInviteePlayerId())) {
            Long inviteePlayerId = parseLongId(req.getInviteePlayerId(), "被邀请球员主键");
            inviteeProfile = playerProfileDao.selectById(inviteePlayerId);
            if (inviteeProfile == null || !NOT_DELETED.equals(inviteeProfile.getIsDelete())) {
                throw new ServiceException("被邀请球员不存在");
            }
            inviteeMemberId = inviteeProfile.getMemberId();
            captainPermissionService.requireCertifiedPlayer(inviteeMemberId);
        } else {
            inviteeMemberId = parseLongId(req.getInviteeMemberId(), "被邀请用户主键");
            inviteeProfile = captainPermissionService.requireCertifiedPlayer(inviteeMemberId);
        }
        if (memberId.equals(inviteeMemberId)) {
            throw new ServiceException("不能邀请自己");
        }
        teamMembershipSupport.assertEligibleToJoinTeam(inviteeMemberId, team.getDivisionId());
        assertTeamHasCapacity(team);
        assertNoPendingInvitation(teamPk, inviteeMemberId);

        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.DAY_OF_MONTH, teamProperties.getInvitation().getExpireDays());

        BasketTeamInvitation invitation = new BasketTeamInvitation();
        invitation.setInvitationNo("IV" + IdUtil.getSnowflakeNextIdStr());
        invitation.setTeamId(teamPk);
        invitation.setInviterMemberId(memberId);
        invitation.setInviteeMemberId(inviteeMemberId);
        invitation.setInviteePlayerId(inviteeProfile.getId());
        invitation.setInvitationMessage(trimToNull(req.getInvitationMessage()));
        invitation.setInvitationStatus(TeamInvitationStatusEnum.PENDING.getCode());
        invitation.setExpireTime(calendar.getTime());
        invitation.setIsDelete(NOT_DELETED);
        invitationDao.insert(invitation);

        businessMessageService.sendSiteMessage(inviteeMemberId, "球队邀请",
                "您收到来自球队「" + team.getTeamName() + "」的邀请",
                BIZ_TYPE_TEAM_INVITE, invitation.getId(), "TEAM_INVITATION", String.valueOf(invitation.getId()),
                "TEAM_INVITATION_RECEIVED", NoticeParams.json("teamName", team.getTeamName()));
        return toItemResp(invitation, team, loadMemberProfileMap(Collections.singleton(inviteeMemberId)), inviteeMemberId);
    }

    @Override
    public IPage<TeamInvitationItemResp> pageTeamInvitations(String teamId, Integer currentPage, Integer pageSize, Integer status) {
        Long teamPk = parseLongId(teamId, "球队主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTeam team = captainPermissionService.requireTeamCaptain(memberId, teamPk);

        long page = normalizePage(currentPage);
        long size = normalizeSize(pageSize);

        LambdaQueryWrapper<BasketTeamInvitation> countWrapper = new LambdaQueryWrapper<BasketTeamInvitation>()
                .eq(BasketTeamInvitation::getTeamId, teamPk)
                .eq(BasketTeamInvitation::getIsDelete, NOT_DELETED);
        if (status != null) {
            countWrapper.eq(BasketTeamInvitation::getInvitationStatus, status);
        }
        long total = invitationDao.selectCount(countWrapper);

        List<BasketTeamInvitation> invitations;
        if (total == 0) {
            invitations = Collections.emptyList();
        } else {
            LambdaQueryWrapper<BasketTeamInvitation> listWrapper = new LambdaQueryWrapper<BasketTeamInvitation>()
                    .eq(BasketTeamInvitation::getTeamId, teamPk)
                    .eq(BasketTeamInvitation::getIsDelete, NOT_DELETED)
                    .orderByDesc(BasketTeamInvitation::getCreateTime);
            if (status != null) {
                listWrapper.eq(BasketTeamInvitation::getInvitationStatus, status);
            }
            listWrapper.last("LIMIT " + (page - 1) * size + ", " + size);
            invitations = invitationDao.selectList(listWrapper);
        }

        Map<Long, BasketMemberProfile> profileMap = loadMemberProfileMap(invitations.stream()
                .map(BasketTeamInvitation::getInviteeMemberId).collect(Collectors.toSet()));

        Page<TeamInvitationItemResp> respPage = new Page<>(page, size, total);
        List<TeamInvitationItemResp> items = new ArrayList<>();
        for (BasketTeamInvitation invitation : invitations) {
            items.add(toItemResp(invitation, team, profileMap, invitation.getInviteeMemberId()));
        }
        respPage.setRecords(items);
        return respPage;
    }

    @Override
    public IPage<TeamInvitationItemResp> pageMyInvitations(Integer currentPage, Integer pageSize, Integer status) {
        Long memberId = UserContext.getRequiredUserId();
        long page = normalizePage(currentPage);
        long size = normalizeSize(pageSize);

        LambdaQueryWrapper<BasketTeamInvitation> countWrapper = new LambdaQueryWrapper<BasketTeamInvitation>()
                .eq(BasketTeamInvitation::getInviteeMemberId, memberId)
                .eq(BasketTeamInvitation::getIsDelete, NOT_DELETED);
        if (status != null) {
            countWrapper.eq(BasketTeamInvitation::getInvitationStatus, status);
        }
        long total = invitationDao.selectCount(countWrapper);

        List<BasketTeamInvitation> invitations;
        if (total == 0) {
            invitations = Collections.emptyList();
        } else {
            long maxPage = Math.max(1L, (total + size - 1) / size);
            if (page > maxPage) {
                page = maxPage;
            }
            LambdaQueryWrapper<BasketTeamInvitation> listWrapper = new LambdaQueryWrapper<BasketTeamInvitation>()
                    .eq(BasketTeamInvitation::getInviteeMemberId, memberId)
                    .eq(BasketTeamInvitation::getIsDelete, NOT_DELETED)
                    .orderByDesc(BasketTeamInvitation::getCreateTime);
            if (status != null) {
                listWrapper.eq(BasketTeamInvitation::getInvitationStatus, status);
            }
            listWrapper.last("LIMIT " + (page - 1) * size + ", " + size);
            invitations = invitationDao.selectList(listWrapper);
        }

        Map<Long, BasketTeam> teamMap = loadTeamMap(invitations.stream()
                .map(BasketTeamInvitation::getTeamId).collect(Collectors.toSet()));
        Map<Long, BasketMemberProfile> profileMap = loadMemberProfileMap(Collections.singleton(memberId));

        Page<TeamInvitationItemResp> respPage = new Page<>(page, size, total);
        List<TeamInvitationItemResp> items = new ArrayList<>();
        for (BasketTeamInvitation invitation : invitations) {
            items.add(toItemResp(invitation, teamMap.get(invitation.getTeamId()), profileMap, memberId));
        }
        respPage.setRecords(items);
        return respPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamInvitationItemResp respond(String invitationId, TeamInvitationRespondReq req) {
        Boolean accept = resolveAccept(req);
        Long invitationPk = parseLongId(invitationId, "邀请主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTeamInvitation invitation = requireOwnedInvitation(invitationPk, memberId);
        expireIfNeeded(invitation);

        TeamInvitationStatusEnum statusEnum = TeamInvitationStatusEnum.fromCode(invitation.getInvitationStatus());
        if (!statusEnum.canRespond()) {
            throw new ServiceException("邀请状态不可响应");
        }

        Date now = new Date();
        int targetStatus = Boolean.TRUE.equals(accept)
                ? TeamInvitationStatusEnum.ACCEPTED.getCode()
                : TeamInvitationStatusEnum.REJECTED.getCode();
        int updated = invitationDao.update(null, new LambdaUpdateWrapper<BasketTeamInvitation>()
                .eq(BasketTeamInvitation::getId, invitationPk)
                .eq(BasketTeamInvitation::getInviteeMemberId, memberId)
                .eq(BasketTeamInvitation::getInvitationStatus, TeamInvitationStatusEnum.PENDING.getCode())
                .eq(BasketTeamInvitation::getIsDelete, NOT_DELETED)
                .set(BasketTeamInvitation::getInvitationStatus, targetStatus)
                .set(BasketTeamInvitation::getRespondedTime, now)
                .set(BasketTeamInvitation::getResponseRemark, trimToNull(req.getResponseRemark())));
        if (updated == 0) {
            throw new ServiceException("邀请状态已变更，请刷新后重试");
        }

        invitation = invitationDao.selectById(invitationPk);
        BasketTeam team = teamDao.selectById(invitation.getTeamId());
        if (Boolean.TRUE.equals(accept)) {
            validateTeamJoinable(team);
            teamMembershipSupport.assertEligibleToJoinTeam(memberId, team.getDivisionId());
            BasketPlayerProfile profile = captainPermissionService.requireCertifiedPlayer(memberId);
            teamMemberJoinHelper.addMember(team, memberId, profile.getId(), com.zhs.enums.TeamJoinSourceEnum.INVITE);
        }

        Map<Long, BasketMemberProfile> profileMap = loadMemberProfileMap(Collections.singleton(memberId));
        return toItemResp(invitation, team, profileMap, memberId);
    }

    private Boolean resolveAccept(TeamInvitationRespondReq req) {
        if (req == null) {
            throw new ServiceException("响应结果不能为空");
        }
        if (req.getAccept() != null) {
            return req.getAccept();
        }
        if (!StringUtils.hasText(req.getAction())) {
            throw new ServiceException("响应结果不能为空");
        }
        String action = req.getAction().trim().toUpperCase();
        if ("ACCEPT".equals(action)) {
            return true;
        }
        if ("REJECT".equals(action)) {
            return false;
        }
        throw new ServiceException("不支持的响应动作");
    }

    private void assertTerminalMatchForInvite(BasketPlayerProfile inviteeProfile, BasketTeam team) {
        if (GameTerminalSupport.isGeneralTeam(team)) {
            return;
        }
        String reason = GameTerminalSupport.buildInviteMismatchMessage(
                inviteeProfile.getGameTerminal(), team.getGameTerminal());
        if (reason != null) {
            throw new ServiceException(reason);
        }
    }

    private void assertTerminalMatchForAccept(BasketPlayerProfile profile, BasketTeam team) {
        if (GameTerminalSupport.isGeneralTeam(team)) {
            return;
        }
        String reason = GameTerminalSupport.buildAcceptMismatchMessage(
                profile.getGameTerminal(), team.getGameTerminal());
        if (reason != null) {
            throw new ServiceException(reason);
        }
    }

    private void assertTeamHasCapacity(BasketTeam team) {
        if (team.getMaxMemberCount() != null && team.getCurrentMemberCount() != null
                && team.getCurrentMemberCount() >= team.getMaxMemberCount()) {
            throw BusinessReason.TEAM_FULL.exception();
        }
    }

    private String resolveInviteePlatformIdentityReason(BasketTeam team, Long inviteeMemberId) {
        if (team == null || inviteeMemberId == null || GameTerminalSupport.isGeneralTeam(team)) {
            return null;
        }
        String platformCode = platformTeamMembershipSupport.resolveTeamPlatformCode(team);
        PlatformIdentityStatusEnum status = platformTeamMembershipSupport.resolvePlatformIdentityStatus(
                inviteeMemberId, platformCode);
        if (status == PlatformIdentityStatusEnum.ACTIVE) {
            return null;
        }
        if (status == PlatformIdentityStatusEnum.PENDING) {
            return "被邀请用户该平台游戏 ID 审核中";
        }
        if (status == PlatformIdentityStatusEnum.REJECTED) {
            return "被邀请用户该平台游戏 ID 已被驳回";
        }
        return "被邀请用户尚未绑定该平台已生效游戏 ID";
    }

    private String resolveInviteeTerminalMismatchReason(BasketTeam team, Long inviteeMemberId) {
        if (team == null || inviteeMemberId == null || GameTerminalSupport.isGeneralTeam(team)) {
            return null;
        }
        BasketPlayerProfile profile = playerProfileDao.selectOne(new LambdaQueryWrapper<BasketPlayerProfile>()
                .eq(BasketPlayerProfile::getMemberId, inviteeMemberId)
                .eq(BasketPlayerProfile::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        if (profile == null) {
            return null;
        }
        return GameTerminalSupport.buildAcceptMismatchMessage(profile.getGameTerminal(), team.getGameTerminal());
    }

    private void assertNoPendingInvitation(Long teamId, Long inviteeMemberId) {
        Long count = invitationDao.selectCount(new LambdaQueryWrapper<BasketTeamInvitation>()
                .eq(BasketTeamInvitation::getTeamId, teamId)
                .eq(BasketTeamInvitation::getInviteeMemberId, inviteeMemberId)
                .eq(BasketTeamInvitation::getInvitationStatus, TeamInvitationStatusEnum.PENDING.getCode())
                .eq(BasketTeamInvitation::getIsDelete, NOT_DELETED));
        if (count != null && count > 0) {
            throw BusinessReason.INVITATION_PENDING.exception();
        }
    }

    private BasketTeamInvitation requireOwnedInvitation(Long invitationId, Long memberId) {
        BasketTeamInvitation invitation = invitationDao.selectById(invitationId);
        if (invitation == null || !NOT_DELETED.equals(invitation.getIsDelete())) {
            throw new ServiceException("邀请不存在");
        }
        if (!memberId.equals(invitation.getInviteeMemberId())) {
            throw new ServiceException("无权响应该邀请", 403);
        }
        return invitation;
    }

    private void validateTeamJoinable(BasketTeam team) {
        if (team == null || !NOT_DELETED.equals(team.getIsDelete())) {
            throw new ServiceException("球队不存在");
        }
        teamWritableSupport.assertTeamWritable(team);
        if (!Objects.equals(TeamAuditStatusEnum.APPROVED.getCode(), team.getAuditStatus())) {
            throw new ServiceException("球队尚未通过认证，暂不可加入");
        }
    }

    private void expireIfNeeded(BasketTeamInvitation invitation) {
        if (!Objects.equals(TeamInvitationStatusEnum.PENDING.getCode(), invitation.getInvitationStatus())) {
            return;
        }
        if (invitation.getExpireTime() != null && invitation.getExpireTime().before(new Date())) {
            invitation.setInvitationStatus(TeamInvitationStatusEnum.EXPIRED.getCode());
            invitationDao.updateById(invitation);
            throw BusinessReason.INVITATION_EXPIRED.exception();
        }
    }

    private TeamInvitationItemResp toItemResp(BasketTeamInvitation invitation, BasketTeam team,
                                              Map<Long, BasketMemberProfile> profileMap, Long inviteeMemberId) {
        TeamInvitationStatusEnum status = TeamInvitationStatusEnum.fromCode(invitation.getInvitationStatus());
        TeamInvitationItemResp resp = new TeamInvitationItemResp();
        resp.setInvitationId(toStringId(invitation.getId()));
        resp.setInvitationNo(invitation.getInvitationNo());
        resp.setTeamId(toStringId(invitation.getTeamId()));
        resp.setTeamName(team == null ? null : team.getTeamName());
        fillInvitationEventContext(resp, team);
        resp.setInviterMemberId(toStringId(invitation.getInviterMemberId()));
        resp.setInviteeMemberId(toStringId(invitation.getInviteeMemberId()));
        resp.setInviterUserId(toStringId(invitation.getInviterMemberId()));
        resp.setInviteeUserId(toStringId(invitation.getInviteeMemberId()));
        BasketMemberProfile profile = profileMap.get(invitation.getInviteeMemberId());
        if (profile != null) {
            resp.setInviteeNickname(profile.getNickname());
            resp.setInviteeAvatarUrl(resolveInviteeAvatarUrl(profile.getAvatarFileId()));
        }
        resp.setInvitationMessage(invitation.getInvitationMessage());
        resp.setInvitationStatus(invitation.getInvitationStatus());
        resp.setInvitationStatusText(status.getDescription());
        boolean canRespond = status.canRespond()
                && (invitation.getExpireTime() == null || invitation.getExpireTime().after(new Date()));
        resp.setCanRespond(canRespond);
        String joinIneligibilityReason = inviteeMemberId == null || team == null
                ? null
                : captainPermissionService.resolveJoinIneligibilityReason(inviteeMemberId,
                platformTeamMembershipSupport.resolveTeamPlatformCode(team));
        String platformIdentityReason = resolveInviteePlatformIdentityReason(team, inviteeMemberId);
        if (platformIdentityReason != null) {
            joinIneligibilityReason = platformIdentityReason;
        }
        String terminalMismatchReason = resolveInviteeTerminalMismatchReason(team, inviteeMemberId);
        if (terminalMismatchReason != null) {
            joinIneligibilityReason = terminalMismatchReason;
        }
        resp.setCanAccept(canRespond && joinIneligibilityReason == null);
        resp.setCannotAcceptReason(canRespond ? joinIneligibilityReason : null);
        resp.setExpireTime(invitation.getExpireTime());
        resp.setCreateTime(invitation.getCreateTime());
        return resp;
    }

    private void fillInvitationEventContext(TeamInvitationItemResp resp, BasketTeam team) {
        if (resp == null || team == null || team.getDivisionId() == null) {
            return;
        }
        resp.setDivisionId(toStringId(team.getDivisionId()));
        BasketDivision division = divisionDao.selectById(team.getDivisionId());
        if (division == null || !Objects.equals(NOT_DELETED, division.getIsDelete())) {
            resp.setDivisionName("赛事信息不可用");
            return;
        }
        resp.setDivisionName(division.getDivisionName());
        resp.setPlatformCode(division.getPlatformCode());
        resp.setGameMode(division.getGameMode());
        if (division.getSeasonId() == null) {
            return;
        }
        BasketSeason season = seasonDao.selectById(division.getSeasonId());
        if (season == null) {
            return;
        }
        resp.setSeasonName(StringUtils.hasText(season.getDisplayTitle()) ? season.getDisplayTitle() : season.getSeasonName());
        if (season.getSeriesId() == null) {
            return;
        }
        BasketSeries series = seriesDao.selectById(season.getSeriesId());
        if (series != null) {
            resp.setSeriesName(series.getSeriesName());
        }
    }

    private Map<Long, BasketTeam> loadTeamMap(Set<Long> teamIds) {
        if (CollectionUtils.isEmpty(teamIds)) {
            return Collections.emptyMap();
        }
        Map<Long, BasketTeam> map = new HashMap<>();
        for (BasketTeam team : teamDao.selectByIds(teamIds)) {
            if (team != null && NOT_DELETED.equals(team.getIsDelete())) {
                map.put(team.getId(), team);
            }
        }
        return map;
    }

    private Map<Long, BasketMemberProfile> loadMemberProfileMap(Set<Long> memberIds) {
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

    private String resolveInviteeAvatarUrl(Long avatarFileId) {
        if (avatarFileId == null) {
            return com.zhs.support.MemberAvatarSupport.resolve(null);
        }
        BasketFileAsset asset = fileAssetDao.selectById(avatarFileId);
        if (asset == null || !NOT_DELETED.equals(asset.getIsDelete())) {
            return com.zhs.support.MemberAvatarSupport.resolve(null);
        }
        return com.zhs.support.MemberAvatarSupport.resolve(asset.getAccessUrl());
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
}
