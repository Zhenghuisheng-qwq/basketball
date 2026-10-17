package com.zhs.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketMemberProfileDao;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.BasketTeamDao;
import com.zhs.dao.BasketTeamJoinApplicationDao;
import com.zhs.dao.v4.BasketDivisionDao;
import com.zhs.dao.v4.BasketSeasonDao;
import com.zhs.dao.v4.BasketSeriesDao;
import com.zhs.enums.TeamAuditStatusEnum;
import com.zhs.enums.TeamJoinApplicationStatusEnum;
import com.zhs.enums.TeamJoinSourceEnum;
import com.zhs.enums.TeamPublicStatusEnum;
import com.zhs.enums.TeamStatusEnum;
import com.zhs.exception.BusinessReason;
import com.zhs.i18n.NoticeParams;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketMemberProfile;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamJoinApplication;
import com.zhs.model.BasketTeamMember;
import com.zhs.model.v4.BasketDivision;
import com.zhs.model.v4.BasketSeason;
import com.zhs.model.v4.BasketSeries;
import com.zhs.request.team.TeamJoinApplicationCreateReq;
import com.zhs.request.team.TeamJoinApplicationProcessReq;
import com.zhs.response.team.TeamJoinApplicationItemResp;
import com.zhs.service.IBusinessMessageService;
import com.zhs.service.ICaptainPermissionService;
import com.zhs.service.ITeamJoinApplicationBusinessService;
import com.zhs.service.team.TeamMemberJoinHelper;
import com.zhs.service.team.TeamMembershipChangeGate;
import com.zhs.support.GameTerminalSupport;
import com.zhs.support.PlatformTeamMembershipSupport;
import com.zhs.support.PlayerPositionSupport;
import com.zhs.support.TeamMembershipSupport;
import com.zhs.support.TeamWritableSupport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TeamJoinApplicationBusinessServiceImpl implements ITeamJoinApplicationBusinessService {

    private static final Integer NOT_DELETED = 0;
    private static final String BIZ_TYPE_TEAM_JOIN = "TEAM_JOIN";

    @Resource
    private BasketTeamJoinApplicationDao joinApplicationDao;
    @Resource
    private BasketTeamDao teamDao;
    @Resource
    private BasketMemberProfileDao memberProfileDao;
    @Resource
    private BasketPlayerProfileDao playerProfileDao;
    @Resource
    private ICaptainPermissionService captainPermissionService;
    @Resource
    private TeamMemberJoinHelper teamMemberJoinHelper;
    @Resource
    private IBusinessMessageService businessMessageService;
    @Resource
    private PlatformTeamMembershipSupport platformTeamMembershipSupport;
    @Resource
    private TeamMembershipSupport teamMembershipSupport;
    @Resource
    private TeamWritableSupport teamWritableSupport;
    @Resource
    private BasketDivisionDao divisionDao;
    @Resource
    private BasketSeasonDao seasonDao;
    @Resource
    private BasketSeriesDao seriesDao;
    @Resource
    private TeamMembershipChangeGate membershipChangeGate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamJoinApplicationItemResp createJoinApplication(String teamId, TeamJoinApplicationCreateReq req) {
        Long teamPk = parseLongId(teamId, "球队主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketPlayerProfile profile = captainPermissionService.requireCertifiedPlayer(memberId);
        BasketTeam team = requireJoinableTeam(teamPk);
        teamMembershipSupport.assertEligibleToJoinTeam(memberId, team.getDivisionId());
        assertNoPendingApplication(teamPk, memberId);

        BasketTeamJoinApplication application = new BasketTeamJoinApplication();
        application.setApplicationNo("JA" + IdUtil.getSnowflakeNextIdStr());
        application.setTeamId(teamPk);
        application.setApplicantMemberId(memberId);
        application.setApplicantPlayerId(profile.getId());
        application.setApplyReason(req == null ? null : trimToNull(req.getApplyReason()));
        application.setApplicationStatus(TeamJoinApplicationStatusEnum.PENDING.getCode());
        application.setIsDelete(NOT_DELETED);
        joinApplicationDao.insert(application);
        membershipChangeGate.trackJoinApplication(application, team);

        businessMessageService.sendSiteMessage(team.getCaptainMemberId(), "球队加入申请",
                "收到新的加入球队申请", BIZ_TYPE_TEAM_JOIN, application.getId(),
                "TEAM_JOIN_APPLICATION", String.valueOf(application.getId()),
                "TEAM_JOIN_APPLICATION_RECEIVED", NoticeParams.json("teamName", team.getTeamName()));
        Map<Long, BasketMemberProfile> profileMap = loadMemberProfileMap(Collections.singleton(memberId));
        Map<Long, BasketPlayerProfile> playerProfileMap = loadPlayerProfileMap(Collections.singleton(memberId));
        return toItemResp(application, team, profileMap, playerProfileMap);
    }

    @Override
    public IPage<TeamJoinApplicationItemResp> pageMyApplications(Integer currentPage, Integer pageSize, Integer status) {
        Long memberId = UserContext.getRequiredUserId();
        long page = normalizePage(currentPage);
        long size = normalizeSize(pageSize);

        LambdaQueryWrapper<BasketTeamJoinApplication> countWrapper = new LambdaQueryWrapper<BasketTeamJoinApplication>()
                .eq(BasketTeamJoinApplication::getApplicantMemberId, memberId)
                .eq(BasketTeamJoinApplication::getIsDelete, NOT_DELETED);
        if (status != null) {
            countWrapper.eq(BasketTeamJoinApplication::getApplicationStatus, status);
        }
        long total = joinApplicationDao.selectCount(countWrapper);

        List<BasketTeamJoinApplication> applications;
        if (total == 0) {
            applications = Collections.emptyList();
        } else {
            long maxPage = Math.max(1L, (total + size - 1) / size);
            if (page > maxPage) {
                page = maxPage;
            }
            LambdaQueryWrapper<BasketTeamJoinApplication> listWrapper = new LambdaQueryWrapper<BasketTeamJoinApplication>()
                    .eq(BasketTeamJoinApplication::getApplicantMemberId, memberId)
                    .eq(BasketTeamJoinApplication::getIsDelete, NOT_DELETED)
                    .orderByDesc(BasketTeamJoinApplication::getCreateTime);
            if (status != null) {
                listWrapper.eq(BasketTeamJoinApplication::getApplicationStatus, status);
            }
            listWrapper.last("LIMIT " + (page - 1) * size + ", " + size);
            applications = joinApplicationDao.selectList(listWrapper);
        }

        Map<Long, BasketTeam> teamMap = loadTeamMap(applications.stream()
                .map(BasketTeamJoinApplication::getTeamId).collect(Collectors.toSet()));
        Map<Long, BasketMemberProfile> profileMap = loadMemberProfileMap(Collections.singleton(memberId));
        Map<Long, BasketPlayerProfile> playerProfileMap = loadPlayerProfileMap(Collections.singleton(memberId));

        Page<TeamJoinApplicationItemResp> respPage = new Page<>(page, size, total);
        List<TeamJoinApplicationItemResp> items = new ArrayList<>();
        for (BasketTeamJoinApplication application : applications) {
            items.add(toItemResp(application, teamMap.get(application.getTeamId()), profileMap, playerProfileMap));
        }
        respPage.setRecords(items);
        return respPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(String applicationId) {
        Long applicationPk = parseLongId(applicationId, "申请主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTeamJoinApplication application = requireOwnedApplication(applicationPk, memberId);
        TeamJoinApplicationStatusEnum status = TeamJoinApplicationStatusEnum.fromCode(application.getApplicationStatus());
        if (!status.canCancel()) {
            throw new ServiceException("申请状态不可取消");
        }
        application.setApplicationStatus(TeamJoinApplicationStatusEnum.CANCELLED.getCode());
        application.setProcessedTime(new Date());
        joinApplicationDao.updateById(application);
        membershipChangeGate.markJoinClosed(applicationPk, false);
    }

    @Override
    public IPage<TeamJoinApplicationItemResp> pageTeamApplications(String teamId, Integer currentPage, Integer pageSize, Integer status) {
        Long teamPk = parseLongId(teamId, "球队主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTeam team = captainPermissionService.requireTeamCaptain(memberId, teamPk);

        LambdaQueryWrapper<BasketTeamJoinApplication> wrapper = new LambdaQueryWrapper<BasketTeamJoinApplication>()
                .eq(BasketTeamJoinApplication::getTeamId, teamPk)
                .eq(BasketTeamJoinApplication::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketTeamJoinApplication::getCreateTime);
        if (status != null) {
            wrapper.eq(BasketTeamJoinApplication::getApplicationStatus, status);
        }

        Page<BasketTeamJoinApplication> page = new Page<>(normalizePage(currentPage), normalizeSize(pageSize));
        IPage<BasketTeamJoinApplication> appPage = joinApplicationDao.selectPage(page, wrapper);
        Set<Long> applicantIds = appPage.getRecords().stream()
                .map(BasketTeamJoinApplication::getApplicantMemberId).collect(Collectors.toSet());
        Map<Long, BasketMemberProfile> profileMap = loadMemberProfileMap(applicantIds);
        Map<Long, BasketPlayerProfile> playerProfileMap = loadPlayerProfileMap(applicantIds);

        Page<TeamJoinApplicationItemResp> respPage = new Page<>(appPage.getCurrent(), appPage.getSize(), appPage.getTotal());
        List<TeamJoinApplicationItemResp> items = new ArrayList<>();
        for (BasketTeamJoinApplication application : appPage.getRecords()) {
            items.add(toItemResp(application, team, profileMap, playerProfileMap));
        }
        respPage.setRecords(items);
        return respPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamJoinApplicationItemResp process(String applicationId, TeamJoinApplicationProcessReq req) {
        Boolean approve = resolveApprove(req);
        Long applicationPk = parseLongId(applicationId, "申请主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTeamJoinApplication application = joinApplicationDao.selectById(applicationPk);
        if (application == null || !NOT_DELETED.equals(application.getIsDelete())) {
            throw new ServiceException("申请不存在");
        }
        BasketTeam team = captainPermissionService.requireTeamCaptain(memberId, application.getTeamId());
        teamWritableSupport.assertTeamWritable(team);
        TeamJoinApplicationStatusEnum status = TeamJoinApplicationStatusEnum.fromCode(application.getApplicationStatus());
        if (!status.canProcess()) {
            throw new ServiceException("申请状态不可处理");
        }

        Date now = new Date();
        int targetStatus = Boolean.TRUE.equals(approve)
                ? TeamJoinApplicationStatusEnum.APPROVED.getCode()
                : TeamJoinApplicationStatusEnum.REJECTED.getCode();
        int updated = joinApplicationDao.update(null, new LambdaUpdateWrapper<BasketTeamJoinApplication>()
                .eq(BasketTeamJoinApplication::getId, applicationPk)
                .eq(BasketTeamJoinApplication::getApplicationStatus, TeamJoinApplicationStatusEnum.PENDING.getCode())
                .eq(BasketTeamJoinApplication::getIsDelete, NOT_DELETED)
                .set(BasketTeamJoinApplication::getApplicationStatus, targetStatus)
                .set(BasketTeamJoinApplication::getProcessedBy, memberId)
                .set(BasketTeamJoinApplication::getProcessedTime, now)
                .set(BasketTeamJoinApplication::getProcessRemark, trimToNull(req.getProcessRemark())));
        if (updated == 0) {
            throw BusinessReason.APPLICATION_STATE_CONFLICT.exception();
        }
        application = joinApplicationDao.selectById(applicationPk);
        if (Boolean.TRUE.equals(approve)) {
            teamMembershipSupport.assertEligibleToJoinTeam(application.getApplicantMemberId(), team.getDivisionId());
            if (membershipChangeGate.shouldHold(team.getId())) {
                membershipChangeGate.markJoinPendingAdmin(application, team, memberId);
                businessMessageService.sendSiteMessage(application.getApplicantMemberId(), "加入申请待审核",
                        "队长已同意您加入球队「" + team.getTeamName() + "」，等待管理员审核后生效",
                        BIZ_TYPE_TEAM_JOIN, application.getId(),
                        "TEAM_JOIN_APPLICATION", String.valueOf(application.getId()),
                        "TEAM_JOIN_APPLICATION_APPROVED", NoticeParams.json("teamName", team.getTeamName()));
            } else {
                teamMemberJoinHelper.addMember(team, application.getApplicantMemberId(),
                        application.getApplicantPlayerId(), TeamJoinSourceEnum.APPLY);
                membershipChangeGate.markJoinEffective(application, team, memberId);
                businessMessageService.sendSiteMessage(application.getApplicantMemberId(), "加入申请已通过",
                        "您的加入球队申请已通过", BIZ_TYPE_TEAM_JOIN, application.getId(),
                        "TEAM_JOIN_APPLICATION", String.valueOf(application.getId()),
                        "TEAM_JOIN_APPLICATION_APPROVED", NoticeParams.json("teamName", team.getTeamName()));
            }
        } else {
            membershipChangeGate.markJoinClosed(applicationPk, true);
            businessMessageService.sendSiteMessage(application.getApplicantMemberId(), "加入申请已驳回",
                    "您的加入球队申请已被驳回", BIZ_TYPE_TEAM_JOIN, application.getId(),
                    "TEAM_JOIN_APPLICATION", String.valueOf(application.getId()),
                    "TEAM_JOIN_APPLICATION_REJECTED", NoticeParams.json(
                            "teamName", team.getTeamName(),
                            "remark", trimToNull(req.getProcessRemark()) == null ? "" : trimToNull(req.getProcessRemark())));
        }

        Map<Long, BasketMemberProfile> profileMap = loadMemberProfileMap(Collections.singleton(application.getApplicantMemberId()));
        Map<Long, BasketPlayerProfile> playerProfileMap = loadPlayerProfileMap(Collections.singleton(application.getApplicantMemberId()));
        return toItemResp(application, team, profileMap, playerProfileMap);
    }

    private void assertTerminalMatchForApply(BasketPlayerProfile profile, BasketTeam team) {
        String reason = GameTerminalSupport.buildApplyJoinMismatchMessage(
                profile.getGameTerminal(), team.getGameTerminal());
        if (reason != null) {
            throw new ServiceException(reason);
        }
    }

    private void assertTerminalMatchForApprove(BasketPlayerProfile profile, BasketTeam team) {
        if (profile == null) {
            throw new ServiceException("申请人比赛终端与球队比赛终端不一致，无法加入");
        }
        String reason = GameTerminalSupport.buildApproveApplicationMismatchMessage(
                profile.getGameTerminal(), team.getGameTerminal());
        if (reason != null) {
            throw new ServiceException(reason);
        }
    }

    private BasketPlayerProfile loadApplicantProfile(Long playerId) {
        if (playerId == null) {
            return null;
        }
        BasketPlayerProfile profile = playerProfileDao.selectById(playerId);
        if (profile == null || !NOT_DELETED.equals(profile.getIsDelete())) {
            return null;
        }
        return profile;
    }

    private BasketTeam requireJoinableTeam(Long teamId) {
        BasketTeam team = teamDao.selectById(teamId);
        if (team == null || !NOT_DELETED.equals(team.getIsDelete())) {
            throw new ServiceException("球队不存在");
        }
        teamWritableSupport.assertTeamWritable(team);
        if (!Objects.equals(TeamAuditStatusEnum.APPROVED.getCode(), team.getAuditStatus())) {
            throw new ServiceException("球队认证审核中，暂不可申请");
        }
        if (!Objects.equals(TeamPublicStatusEnum.PUBLIC.getCode(), team.getPublicStatus())) {
            throw new ServiceException("球队已停止招募");
        }
        int maxMembers = team.getMaxMemberCount() != null ? team.getMaxMemberCount() : 8;
        int currentMembers = team.getCurrentMemberCount() != null ? team.getCurrentMemberCount() : 0;
        if (currentMembers >= maxMembers) {
            throw BusinessReason.TEAM_FULL_APPLY.exception();
        }
        return team;
    }

    private void assertNoPendingApplication(Long teamId, Long memberId) {
        Long count = joinApplicationDao.selectCount(new LambdaQueryWrapper<BasketTeamJoinApplication>()
                .eq(BasketTeamJoinApplication::getTeamId, teamId)
                .eq(BasketTeamJoinApplication::getApplicantMemberId, memberId)
                .eq(BasketTeamJoinApplication::getApplicationStatus, TeamJoinApplicationStatusEnum.PENDING.getCode())
                .eq(BasketTeamJoinApplication::getIsDelete, NOT_DELETED));
        if (count != null && count > 0) {
            throw BusinessReason.APPLICATION_PENDING.exception();
        }
    }

    private BasketTeamJoinApplication requireOwnedApplication(Long applicationId, Long memberId) {
        BasketTeamJoinApplication application = joinApplicationDao.selectById(applicationId);
        if (application == null || !NOT_DELETED.equals(application.getIsDelete())) {
            throw new ServiceException("申请不存在");
        }
        if (!memberId.equals(application.getApplicantMemberId())) {
            throw new ServiceException("无权操作该申请", 403);
        }
        return application;
    }

    private TeamJoinApplicationItemResp toItemResp(BasketTeamJoinApplication application, BasketTeam team,
                                                   Map<Long, BasketMemberProfile> profileMap,
                                                   Map<Long, BasketPlayerProfile> playerProfileMap) {
        TeamJoinApplicationStatusEnum status = TeamJoinApplicationStatusEnum.fromCode(application.getApplicationStatus());
        TeamJoinApplicationItemResp resp = new TeamJoinApplicationItemResp();
        resp.setApplicationId(toStringId(application.getId()));
        resp.setApplicationNo(application.getApplicationNo());
        resp.setTeamId(toStringId(application.getTeamId()));
        resp.setTeamName(team == null ? null : team.getTeamName());
        fillApplicationEventContext(resp, team);
        resp.setApplicantMemberId(toStringId(application.getApplicantMemberId()));
        resp.setApplicantUserId(toStringId(application.getApplicantMemberId()));
        BasketMemberProfile profile = profileMap.get(application.getApplicantMemberId());
        resp.setApplicantNickname(profile == null ? null : profile.getNickname());
        BasketPlayerProfile playerProfile = playerProfileMap.get(application.getApplicantMemberId());
        if (playerProfile != null) {
            resp.setPlayerNo(playerProfile.getPlayerNo());
            resp.setPositionCode(playerProfile.getPositionCode());
            resp.setPositionName(PlayerPositionSupport.resolveName(playerProfile.getPositionCode()));
            resp.setGameTerminal(playerProfile.getGameTerminal());
            resp.setCertificationStatus(playerProfile.getCertificationStatus());
        }
        resp.setApplyReason(application.getApplyReason());
        resp.setApplicationStatus(application.getApplicationStatus());
        resp.setApplicationStatusText(status.getDescription());
        resp.setCanCancel(status.canCancel());
        resp.setCanProcess(status.canProcess());
        resp.setProcessRemark(application.getProcessRemark());
        resp.setCreateTime(application.getCreateTime());
        return resp;
    }

    private void fillApplicationEventContext(TeamJoinApplicationItemResp resp, BasketTeam team) {
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

    private Map<Long, BasketPlayerProfile> loadPlayerProfileMap(Set<Long> memberIds) {
        if (CollectionUtils.isEmpty(memberIds)) {
            return Collections.emptyMap();
        }
        Map<Long, BasketPlayerProfile> map = new HashMap<>();
        for (BasketPlayerProfile profile : playerProfileDao.selectList(new LambdaQueryWrapper<BasketPlayerProfile>()
                .in(BasketPlayerProfile::getMemberId, memberIds)
                .eq(BasketPlayerProfile::getIsDelete, NOT_DELETED))) {
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

    private Boolean resolveApprove(TeamJoinApplicationProcessReq req) {
        if (req == null) {
            throw new ServiceException("处理结果不能为空");
        }
        if (req.getApprove() != null) {
            return req.getApprove();
        }
        if (req.getAction() == null || req.getAction().isBlank()) {
            throw new ServiceException("处理结果不能为空");
        }
        String action = req.getAction().trim().toUpperCase();
        if ("APPROVE".equals(action)) {
            return true;
        }
        if ("REJECT".equals(action)) {
            return false;
        }
        throw new ServiceException("不支持的处理动作");
    }

}
