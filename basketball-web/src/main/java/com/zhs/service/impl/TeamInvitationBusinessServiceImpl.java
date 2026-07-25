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
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.BasketTeamInvitationDao;
import com.zhs.enums.TeamAuditStatusEnum;
import com.zhs.enums.TeamInvitationStatusEnum;
import com.zhs.enums.TeamStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketMemberProfile;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamInvitation;
import com.zhs.request.team.TeamInvitationCreateReq;
import com.zhs.request.team.TeamInvitationRespondReq;
import com.zhs.response.team.TeamInvitationItemResp;
import com.zhs.service.IBusinessMessageService;
import com.zhs.service.ICaptainPermissionService;
import com.zhs.service.ITeamInvitationBusinessService;
import com.zhs.service.team.TeamMemberJoinHelper;
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
    private ICaptainPermissionService captainPermissionService;
    @Resource
    private TeamMemberJoinHelper teamMemberJoinHelper;
    @Resource
    private IBusinessMessageService businessMessageService;
    @Resource
    private AccountTeamProperties teamProperties;
    @Resource
    private BasketPlayerProfileDao playerProfileDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamInvitationItemResp createInvitation(String teamId, TeamInvitationCreateReq req) {
        if (req == null || (!StringUtils.hasText(req.getInviteePlayerId()) && !StringUtils.hasText(req.getInviteeMemberId()))) {
            throw new ServiceException("被邀请球员不能为空");
        }
        Long teamPk = parseLongId(teamId, "球队主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTeam team = captainPermissionService.requireTeamCaptain(memberId, teamPk);

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
            inviteeMemberId = parseLongId(req.getInviteeMemberId(), "被邀请会员主键");
            inviteeProfile = captainPermissionService.requireCertifiedPlayer(inviteeMemberId);
        }
        if (memberId.equals(inviteeMemberId)) {
            throw new ServiceException("不能邀请自己");
        }
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
                BIZ_TYPE_TEAM_INVITE, invitation.getId(), "TEAM_INVITATION", String.valueOf(invitation.getId()));
        return toItemResp(invitation, team, loadMemberProfileMap(Collections.singleton(inviteeMemberId)), inviteeMemberId);
    }

    @Override
    public IPage<TeamInvitationItemResp> pageTeamInvitations(String teamId, Integer currentPage, Integer pageSize, Integer status) {
        Long teamPk = parseLongId(teamId, "球队主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTeam team = captainPermissionService.requireTeamCaptain(memberId, teamPk);

        LambdaQueryWrapper<BasketTeamInvitation> wrapper = new LambdaQueryWrapper<BasketTeamInvitation>()
                .eq(BasketTeamInvitation::getTeamId, teamPk)
                .eq(BasketTeamInvitation::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketTeamInvitation::getCreateTime);
        if (status != null) {
            wrapper.eq(BasketTeamInvitation::getInvitationStatus, status);
        }

        Page<BasketTeamInvitation> page = new Page<>(normalizePage(currentPage), normalizeSize(pageSize));
        IPage<BasketTeamInvitation> invitationPage = invitationDao.selectPage(page, wrapper);
        Map<Long, BasketMemberProfile> profileMap = loadMemberProfileMap(invitationPage.getRecords().stream()
                .map(BasketTeamInvitation::getInviteeMemberId).collect(Collectors.toSet()));

        Page<TeamInvitationItemResp> respPage = new Page<>(invitationPage.getCurrent(), invitationPage.getSize(), invitationPage.getTotal());
        List<TeamInvitationItemResp> items = new ArrayList<>();
        for (BasketTeamInvitation invitation : invitationPage.getRecords()) {
            items.add(toItemResp(invitation, team, profileMap, invitation.getInviteeMemberId()));
        }
        respPage.setRecords(items);
        return respPage;
    }

    @Override
    public IPage<TeamInvitationItemResp> pageMyInvitations(Integer currentPage, Integer pageSize, Integer status) {
        Long memberId = UserContext.getRequiredUserId();
        LambdaQueryWrapper<BasketTeamInvitation> wrapper = new LambdaQueryWrapper<BasketTeamInvitation>()
                .eq(BasketTeamInvitation::getInviteeMemberId, memberId)
                .eq(BasketTeamInvitation::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketTeamInvitation::getCreateTime);
        if (status != null) {
            wrapper.eq(BasketTeamInvitation::getInvitationStatus, status);
        }

        Page<BasketTeamInvitation> page = new Page<>(normalizePage(currentPage), normalizeSize(pageSize));
        IPage<BasketTeamInvitation> invitationPage = invitationDao.selectPage(page, wrapper);
        Map<Long, BasketTeam> teamMap = loadTeamMap(invitationPage.getRecords().stream()
                .map(BasketTeamInvitation::getTeamId).collect(Collectors.toSet()));
        Map<Long, BasketMemberProfile> profileMap = loadMemberProfileMap(Collections.singleton(memberId));

        Page<TeamInvitationItemResp> respPage = new Page<>(invitationPage.getCurrent(), invitationPage.getSize(), invitationPage.getTotal());
        List<TeamInvitationItemResp> items = new ArrayList<>();
        for (BasketTeamInvitation invitation : invitationPage.getRecords()) {
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
            captainPermissionService.assertEligibleToJoinTeam(memberId);
            validateTeamJoinable(team);
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

    private void assertNoPendingInvitation(Long teamId, Long inviteeMemberId) {
        Long count = invitationDao.selectCount(new LambdaQueryWrapper<BasketTeamInvitation>()
                .eq(BasketTeamInvitation::getTeamId, teamId)
                .eq(BasketTeamInvitation::getInviteeMemberId, inviteeMemberId)
                .eq(BasketTeamInvitation::getInvitationStatus, TeamInvitationStatusEnum.PENDING.getCode())
                .eq(BasketTeamInvitation::getIsDelete, NOT_DELETED));
        if (count != null && count > 0) {
            throw new ServiceException("已存在待处理的邀请");
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
        if (!Objects.equals(TeamStatusEnum.ACTIVE.getCode(), team.getTeamStatus())) {
            throw new ServiceException("球队不可加入");
        }
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
            throw new ServiceException("邀请已过期");
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
        resp.setInviterMemberId(toStringId(invitation.getInviterMemberId()));
        resp.setInviteeMemberId(toStringId(invitation.getInviteeMemberId()));
        BasketMemberProfile profile = profileMap.get(invitation.getInviteeMemberId());
        resp.setInviteeNickname(profile == null ? null : profile.getNickname());
        resp.setInvitationMessage(invitation.getInvitationMessage());
        resp.setInvitationStatus(invitation.getInvitationStatus());
        resp.setInvitationStatusText(status.getDescription());
        boolean canRespond = status.canRespond()
                && (invitation.getExpireTime() == null || invitation.getExpireTime().after(new Date()));
        resp.setCanRespond(canRespond);
        String joinIneligibilityReason = inviteeMemberId == null
                ? null
                : captainPermissionService.resolveJoinIneligibilityReason(inviteeMemberId);
        resp.setCanAccept(canRespond && joinIneligibilityReason == null);
        resp.setCannotAcceptReason(canRespond ? joinIneligibilityReason : null);
        resp.setExpireTime(invitation.getExpireTime());
        resp.setCreateTime(invitation.getCreateTime());
        return resp;
    }

    private Map<Long, BasketTeam> loadTeamMap(Set<Long> teamIds) {
        if (CollectionUtils.isEmpty(teamIds)) {
            return Collections.emptyMap();
        }
        Map<Long, BasketTeam> map = new HashMap<>();
        for (BasketTeam team : teamDao.selectBatchIds(teamIds)) {
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
