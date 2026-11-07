package com.zhs.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.common.UserContext;
import com.zhs.config.AccountTeamProperties;
import com.zhs.dao.BasketFileAssetDao;
import com.zhs.dao.BasketHonorRecordDao;
import com.zhs.dao.BasketMemberProfileDao;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.BasketTeamAuditRecordDao;
import com.zhs.dao.BasketTeamDao;
import com.zhs.dao.BasketTeamInvitationDao;
import com.zhs.dao.BasketTeamJoinApplicationDao;
import com.zhs.dao.BasketPlayerPlatformAccountDao;
import com.zhs.dao.BasketTeamMemberDao;
import com.zhs.enums.FileUsageTypeEnum;
import com.zhs.enums.PlatformIdentityStatusEnum;
import com.zhs.enums.TeamAuditStatusEnum;
import com.zhs.enums.TeamInvitationStatusEnum;
import com.zhs.enums.TeamJoinApplicationStatusEnum;
import com.zhs.enums.TeamJoinSourceEnum;
import com.zhs.enums.TeamMemberRoleEnum;
import com.zhs.enums.TeamMembershipStatusEnum;
import com.zhs.enums.TeamPublicStatusEnum;
import com.zhs.enums.TeamRosterTypeEnum;
import com.zhs.enums.TeamStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.dao.v4.BasketDivisionDao;
import com.zhs.dao.v4.BasketEventTeamDao;
import com.zhs.dao.v4.BasketPersonalDrawTeamDao;
import com.zhs.dao.v4.BasketRegistrationDao;
import com.zhs.dao.v4.BasketSeasonDao;
import com.zhs.dao.v4.BasketSeriesDao;
import com.zhs.enums.v4.DivisionStatusEnum;
import com.zhs.enums.v4.SeasonStatusEnum;
import com.zhs.enums.v4.FormationStatusEnum;
import com.zhs.enums.v4.RegistrationStatusEnum;
import com.zhs.model.v4.BasketCaptainGrant;
import com.zhs.model.v4.BasketDivision;
import com.zhs.model.v4.BasketEventTeam;
import com.zhs.model.v4.BasketPersonalDrawTeam;
import com.zhs.model.v4.BasketRegistration;
import com.zhs.model.v4.BasketSeason;
import com.zhs.model.v4.BasketSeries;
import com.zhs.model.BasketFileAsset;
import com.zhs.model.BasketHonorRecord;
import com.zhs.model.BasketPlayerPlatformAccount;
import com.zhs.model.BasketMemberProfile;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamAuditRecord;
import com.zhs.model.BasketTeamInvitation;
import com.zhs.model.BasketTeamJoinApplication;
import com.zhs.model.BasketTeamMember;
import com.zhs.request.team.TeamCreateReq;
import com.zhs.request.team.TeamLogoBindReq;
import com.zhs.request.team.TeamUpdateReq;
import com.zhs.response.team.InheritableTeamSummaryResp;
import com.zhs.response.team.JoinableTeamPageResp;
import com.zhs.response.team.TeamDetailResp;
import com.zhs.response.team.TeamSummaryResp;
import com.zhs.support.CaptainGrantSupport;
import com.zhs.support.GameTerminalSupport;
import com.zhs.support.PlatformTeamMembershipSupport;
import com.zhs.support.TeamMembershipSupport;
import com.zhs.support.TeamWritableSupport;
import com.zhs.service.IAccountFileService;
import com.zhs.service.IAccountTeamService;
import com.zhs.service.ICaptainPermissionService;
import com.zhs.util.AccountSecurityLogHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AccountTeamServiceImpl implements IAccountTeamService {

    private static final Integer NOT_DELETED = 0;
    private static final int IS_CURRENT = 1;
    private static final String EVENT_TEAM_CREATE = "TEAM_CREATE";
    private static final String EVENT_TEAM_UPDATE = "TEAM_UPDATE";
    private static final String EVENT_TEAM_LOGO = "TEAM_LOGO";

    @Resource
    private BasketTeamDao teamDao;
    @Resource
    private BasketTeamAuditRecordDao teamAuditRecordDao;
    @Resource
    private BasketTeamMemberDao teamMemberDao;
    @Resource
    private BasketTeamJoinApplicationDao joinApplicationDao;
    @Resource
    private BasketTeamInvitationDao invitationDao;
    @Resource
    private com.zhs.service.team.TeamMembershipChangeGate membershipChangeGate;
    @Resource
    private BasketPlayerPlatformAccountDao platformAccountDao;
    @Resource
    private PlatformTeamMembershipSupport platformTeamMembershipSupport;
    @Resource
    private TeamMembershipSupport teamMembershipSupport;
    @Resource
    private CaptainGrantSupport captainGrantSupport;
    @Resource
    private TeamWritableSupport teamWritableSupport;
    @Resource
    private BasketPlayerProfileDao playerProfileDao;
    @Resource
    private BasketMemberProfileDao memberProfileDao;
    @Resource
    private BasketFileAssetDao fileAssetDao;
    @Resource
    private BasketHonorRecordDao honorRecordDao;
    @Resource
    private ICaptainPermissionService captainPermissionService;
    @Resource
    private IAccountFileService accountFileService;
    @Resource
    private AccountSecurityLogHelper securityLogHelper;
    @Resource
    private AccountTeamProperties teamProperties;
    @Resource
    private BasketPersonalDrawTeamDao personalDrawTeamDao;
    @Resource
    private BasketDivisionDao divisionDao;
    @Resource
    private BasketSeasonDao seasonDao;
    @Resource
    private BasketSeriesDao seriesDao;
    @Resource
    private BasketRegistrationDao registrationDao;
    @Resource
    private BasketEventTeamDao eventTeamDao;

    @Override
    public IPage<TeamSummaryResp> pageMine(Integer currentPage, Integer pageSize) {
        return pageMine(currentPage, pageSize, "current");
    }

    @Override
    public IPage<TeamSummaryResp> pageMine(Integer currentPage, Integer pageSize, String scope) {
        Long memberId = UserContext.getRequiredUserId();
        boolean history = scope != null && "history".equalsIgnoreCase(scope.trim());
        // scope 为空或非法时默认 current，兼容旧调用

        List<BasketTeamMember> allMemberships = teamMemberDao.selectList(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getMemberId, memberId)
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketTeamMember::getJoinTime)
                .orderByDesc(BasketTeamMember::getId));
        if (CollectionUtils.isEmpty(allMemberships)) {
            return emptyPage(currentPage, pageSize);
        }

        // 每个 team_id 只取最新一条成员记录，避免重复卡片
        Map<Long, BasketTeamMember> latestByTeam = new HashMap<>();
        for (BasketTeamMember membership : allMemberships) {
            latestByTeam.putIfAbsent(membership.getTeamId(), membership);
        }
        List<BasketTeam> teams = loadTeams(latestByTeam.keySet());
        Map<Long, BasketTeam> teamMap = teams.stream()
                .collect(Collectors.toMap(BasketTeam::getId, t -> t, (a, b) -> a));
        EventContextMaps ctx = loadEventContextMaps(teams, membershipDivisionIds(latestByTeam.values()));
        Map<Long, String> logoUrlMap = loadLogoUrlMap(teams);
        Map<Long, Map<Long, String>> registrationStatusMap = loadRegistrationStatusMap(teams);

        List<TeamSummaryResp> items = new ArrayList<>();
        for (Map.Entry<Long, BasketTeamMember> entry : latestByTeam.entrySet()) {
            BasketTeam team = teamMap.get(entry.getKey());
            if (team == null) {
                continue;
            }
            boolean isCurrentTeam = isCurrentTeamForUser(team, entry.getValue(), ctx);
            if (history == isCurrentTeam) {
                continue;
            }
            TeamSummaryResp resp = toSummaryResp(team, entry.getValue(), logoUrlMap.get(team.getLogoFileId()));
            enrichTeamSummary(resp, team, entry.getValue(), memberId, ctx,
                    registrationStatus(registrationStatusMap, team.getId(), displayDivisionId(team, entry.getValue())),
                    !isCurrentTeam);
            items.add(resp);
        }
        if (history) {
            items.sort((a, b) -> {
                Date at = a.getCreateTime();
                Date bt = b.getCreateTime();
                if (at == null && bt == null) return 0;
                if (at == null) return 1;
                if (bt == null) return -1;
                return bt.compareTo(at);
            });
        }
        return toPage(items, currentPage, pageSize);
    }

    @Override
    public JoinableTeamPageResp pageJoinable(Integer currentPage, Integer pageSize, String platformCode, String keyword) {
        Long memberId = UserContext.getRequiredUserId();
        long page = normalizePage(currentPage);
        long size = normalizeSize(pageSize);

        List<BasketTeamMember> activeMemberships = teamMembershipSupport.listActiveMemberships(memberId);
        Set<Long> joinedTeamIds = activeMemberships.stream()
                .map(BasketTeamMember::getTeamId)
                .collect(Collectors.toSet());
        Set<Long> joinedDivisionIds = activeMemberships.stream()
                .map(BasketTeamMember::getDivisionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        BasketTeamMember firstMembership = CollectionUtils.isEmpty(activeMemberships) ? null : activeMemberships.get(0);
        BasketTeam currentTeam = firstMembership == null ? null : teamDao.selectById(firstMembership.getTeamId());
        Integer pendingApplicationStatus = teamMembershipSupport.findPendingApplicationStatus(memberId);
        boolean hasActiveTeam = !CollectionUtils.isEmpty(activeMemberships);

        captainPermissionService.requireCertifiedPlayer(memberId);

        List<BasketDivision> openDivisions = excludeEndedDivisions(divisionDao.selectList(
                new LambdaQueryWrapper<BasketDivision>()
                        .eq(BasketDivision::getIsDelete, NOT_DELETED)
                        .notIn(BasketDivision::getDivisionStatus,
                                DivisionStatusEnum.FINISHED.getCode(),
                                DivisionStatusEnum.ARCHIVED.getCode())));
        if (CollectionUtils.isEmpty(openDivisions)) {
            return fillJoinablePageMeta(
                    emptyJoinablePage(page, size, "暂无可加入的赛事球队"),
                    hasActiveTeam, currentTeam, pendingApplicationStatus);
        }
        Set<Long> openDivisionIds = openDivisions.stream()
                .map(BasketDivision::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        LambdaQueryWrapper<BasketTeam> countWrapper = buildJoinableTeamWrapper(openDivisionIds, joinedTeamIds, keyword);
        Long totalCount = teamDao.selectCount(countWrapper);
        long total = totalCount == null ? 0L : totalCount;
        if (total <= 0) {
            return fillJoinablePageMeta(
                    emptyJoinablePage(page, size, "暂无可加入的赛事球队"),
                    hasActiveTeam, currentTeam, pendingApplicationStatus);
        }

        long maxPage = Math.max(1L, (total + size - 1) / size);
        if (page > maxPage) {
            page = maxPage;
        }
        long offset = (page - 1) * size;
        LambdaQueryWrapper<BasketTeam> pageWrapper = buildJoinableTeamWrapper(openDivisionIds, joinedTeamIds, keyword)
                .orderByDesc(BasketTeam::getCreateTime)
                .orderByDesc(BasketTeam::getId)
                // 项目未启用 MyBatis-Plus 分页插件，显式 LIMIT 做后端分页
                .last("LIMIT " + offset + "," + size);
        List<BasketTeam> pageTeams = teamDao.selectList(pageWrapper);
        if (pageTeams == null) {
            pageTeams = Collections.emptyList();
        }

        Set<Long> pendingTeamIds = loadPendingApplicationTeamIds(memberId);
        EventContextMaps ctx = loadEventContextMaps(pageTeams);
        Map<Long, String> logoUrlMap = loadLogoUrlMap(pageTeams);
        Map<Long, String> captainNameMap = loadCaptainNameMap(pageTeams);
        List<TeamSummaryResp> items = pageTeams.stream()
                .map(team -> {
                    TeamSummaryResp resp = toJoinableSummaryResp(team, logoUrlMap.get(team.getLogoFileId()),
                            captainNameMap.get(team.getCaptainMemberId()),
                            joinedDivisionIds.contains(team.getDivisionId()),
                            pendingApplicationStatus, pendingTeamIds);
                    fillEventContext(resp, team, ctx);
                    return resp;
                })
                .collect(Collectors.toList());

        JoinableTeamPageResp respPage = new JoinableTeamPageResp(page, size, total);
        respPage.setRecords(items);
        return fillJoinablePageMeta(respPage, hasActiveTeam, currentTeam, pendingApplicationStatus);
    }

    /**
     * 赛事是否已结束以后台填写的比赛结束时间为准：先用赛区比赛结束时间，没填则用赛季比赛结束时间。
     * 赛区状态仍可能停在报名截止，不能用来判断赛事是否结束。
     */
    private List<BasketDivision> excludeEndedDivisions(List<BasketDivision> divisions) {
        if (CollectionUtils.isEmpty(divisions)) {
            return divisions == null ? Collections.emptyList() : divisions;
        }
        Set<Long> seasonIds = divisions.stream()
                .map(BasketDivision::getSeasonId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, BasketSeason> seasons = new HashMap<>();
        if (!seasonIds.isEmpty()) {
            List<BasketSeason> rows = seasonDao.selectByIds(seasonIds);
            if (rows != null) {
                for (BasketSeason season : rows) {
                    if (season != null && season.getId() != null && NOT_DELETED.equals(season.getIsDelete())) {
                        seasons.put(season.getId(), season);
                    }
                }
            }
        }
        Date now = new Date();
        return divisions.stream()
                .filter(division -> !eventMatchPeriodEnded(division, seasons.get(division.getSeasonId()), now))
                .collect(Collectors.toList());
    }

    private boolean eventMatchPeriodEnded(BasketDivision division, BasketSeason season, Date now) {
        if (division == null || now == null) {
            return false;
        }
        Date end = division.getMatchEndTime();
        if (end == null && season != null) {
            end = season.getMatchEndTime();
        }
        if (end != null) {
            return now.after(end);
        }
        if (DivisionStatusEnum.FINISHED.getCode().equals(division.getDivisionStatus())
                || DivisionStatusEnum.ARCHIVED.getCode().equals(division.getDivisionStatus())) {
            return true;
        }
        return season != null && (SeasonStatusEnum.FINISHED.getCode().equals(season.getSeasonStatus())
                || SeasonStatusEnum.ARCHIVED.getCode().equals(season.getSeasonStatus()));
    }

    private LambdaQueryWrapper<BasketTeam> buildJoinableTeamWrapper(Set<Long> openDivisionIds,
                                                                   Set<Long> joinedTeamIds,
                                                                   String keyword) {
        LambdaQueryWrapper<BasketTeam> wrapper = new LambdaQueryWrapper<BasketTeam>()
                .eq(BasketTeam::getIsDelete, NOT_DELETED)
                .eq(BasketTeam::getTeamStatus, TeamStatusEnum.ACTIVE.getCode())
                .eq(BasketTeam::getAuditStatus, TeamAuditStatusEnum.APPROVED.getCode())
                .eq(BasketTeam::getPublicStatus, TeamPublicStatusEnum.PUBLIC.getCode())
                .in(BasketTeam::getDivisionId, openDivisionIds);
        if (!joinedTeamIds.isEmpty()) {
            wrapper.notIn(BasketTeam::getId, joinedTeamIds);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(BasketTeam::getTeamName, kw).or().like(BasketTeam::getShortName, kw));
        }
        return wrapper;
    }

    private JoinableTeamPageResp fillJoinablePageMeta(JoinableTeamPageResp respPage,
                                                      boolean hasActiveTeam,
                                                      BasketTeam currentTeam,
                                                      Integer pendingApplicationStatus) {
        respPage.setPlatformCode(null);
        respPage.setPlatformIdentityStatus(null);
        respPage.setHasActiveTeam(hasActiveTeam);
        respPage.setCurrentTeamId(currentTeam == null ? null : toStringId(currentTeam.getId()));
        respPage.setCurrentTeamName(currentTeam == null ? null : currentTeam.getTeamName());
        respPage.setPendingApplicationStatus(pendingApplicationStatus);
        respPage.setCanApply(true);
        respPage.setDisabledReason(null);
        return respPage;
    }

    private Map<Long, String> loadCaptainNameMap(List<BasketTeam> teams) {
        Set<Long> captainMemberIds = teams.stream()
                .map(BasketTeam::getCaptainMemberId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (captainMemberIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> map = new HashMap<>();
        for (BasketMemberProfile profile : memberProfileDao.selectList(new LambdaQueryWrapper<BasketMemberProfile>()
                .in(BasketMemberProfile::getMemberId, captainMemberIds)
                .eq(BasketMemberProfile::getIsDelete, NOT_DELETED))) {
            if (profile != null && profile.getMemberId() != null) {
                map.put(profile.getMemberId(), profile.getNickname());
            }
        }
        return map;
    }

    @Override
    public List<InheritableTeamSummaryResp> listInheritableTeams(Long divisionId) {
        Long memberId = UserContext.getRequiredUserId();
        Long targetDivisionId = resolveInheritDivisionId(memberId, divisionId, false);
        if (targetDivisionId == null) {
            return Collections.emptyList();
        }
        List<BasketTeam> teams = teamDao.selectList(new LambdaQueryWrapper<BasketTeam>()
                .eq(BasketTeam::getCaptainMemberId, memberId)
                .eq(BasketTeam::getIsDelete, NOT_DELETED)
                .in(BasketTeam::getTeamStatus,
                        TeamStatusEnum.ACTIVE.getCode(), TeamStatusEnum.ARCHIVED.getCode())
                .orderByDesc(BasketTeam::getUpdateTime)
                .orderByDesc(BasketTeam::getId));
        if (CollectionUtils.isEmpty(teams)) {
            return Collections.emptyList();
        }
        List<InheritableTeamSummaryResp> result = new ArrayList<>();
        for (BasketTeam team : teams) {
            if (Objects.equals(team.getDivisionId(), targetDivisionId)) {
                continue;
            }
            InheritableTeamSummaryResp item = new InheritableTeamSummaryResp();
            item.setTeamId(toStringId(team.getId()));
            item.setTeamNo(team.getTeamNo());
            item.setTeamName(team.getTeamName());
            item.setShortName(team.getShortName());
            item.setLogoUrl(resolveLogoUrl(team.getLogoFileId()));
            item.setAuditStatus(team.getAuditStatus());
            TeamAuditStatusEnum auditStatus = safeAuditStatus(team.getAuditStatus());
            item.setAuditStatusText(auditStatus == null ? null : auditStatus.getDescription());
            item.setCurrentMemberCount(team.getCurrentMemberCount());
            item.setPreviousEventName(previousEventName(team));
            item.setHonorCount(countTeamHonors(team.getId()));
            String reason = inheritBlockReason(team, targetDivisionId, memberId);
            item.setCanInherit(reason == null);
            item.setCannotInheritReason(reason);
            result.add(item);
        }
        markRecommendedInheritTeam(result);
        return result;
    }

    /**
     * 默认推荐最近一次报名审核通过、且当前仍能继承的球队。
     * 只创建、草稿、驳回或取消不改变推荐。其余可管理的旧队留给历史选择。
     */
    private void markRecommendedInheritTeam(List<InheritableTeamSummaryResp> result) {
        if (CollectionUtils.isEmpty(result)) {
            return;
        }
        List<Long> teamIds = new ArrayList<>();
        for (InheritableTeamSummaryResp item : result) {
            Long teamId = parseLongIdQuiet(item.getTeamId());
            if (teamId != null) {
                teamIds.add(teamId);
            }
        }
        Map<Long, Date> approvedAt = latestApprovedRegistrationTime(teamIds);
        result.sort((left, right) -> {
            Date leftTime = approvedAt.get(parseLongIdQuiet(left.getTeamId()));
            Date rightTime = approvedAt.get(parseLongIdQuiet(right.getTeamId()));
            if (leftTime == null && rightTime == null) {
                return 0;
            }
            if (leftTime == null) {
                return 1;
            }
            if (rightTime == null) {
                return -1;
            }
            return rightTime.compareTo(leftTime);
        });
        boolean picked = false;
        for (InheritableTeamSummaryResp item : result) {
            Date approvedTime = approvedAt.get(parseLongIdQuiet(item.getTeamId()));
            item.setLastApprovedTime(approvedTime);
            boolean recommend = !picked && Boolean.TRUE.equals(item.getCanInherit()) && approvedTime != null;
            item.setRecommended(recommend);
            if (recommend) {
                picked = true;
            }
        }
    }

    private Map<Long, Date> latestApprovedRegistrationTime(List<Long> teamIds) {
        if (CollectionUtils.isEmpty(teamIds)) {
            return Collections.emptyMap();
        }
        List<BasketRegistration> rows = registrationDao.selectList(new LambdaQueryWrapper<BasketRegistration>()
                .in(BasketRegistration::getTeamId, teamIds)
                .eq(BasketRegistration::getRegistrationStatus, RegistrationStatusEnum.APPROVED.getCode())
                .eq(BasketRegistration::getIsDelete, NOT_DELETED)
                .isNotNull(BasketRegistration::getApproveTime)
                .orderByDesc(BasketRegistration::getApproveTime)
                .orderByDesc(BasketRegistration::getId));
        Map<Long, Date> latest = new HashMap<>();
        for (BasketRegistration row : rows) {
            if (row.getTeamId() != null && row.getApproveTime() != null) {
                latest.putIfAbsent(row.getTeamId(), row.getApproveTime());
            }
        }
        return latest;
    }

    private Long parseLongIdQuiet(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamDetailResp bindGrantToTeam(String teamId, Long divisionId, String clientIp) {
        Long memberId = UserContext.getRequiredUserId();
        Long teamPk = parseLongId(teamId, "球队主键");
        Long targetDivisionId = resolveInheritDivisionId(memberId, divisionId, true);
        BasketDivision division = teamWritableSupport.requireDivisionForTeamCreate(targetDivisionId);
        BasketPlayerProfile playerProfile = captainPermissionService.requireCertifiedPlayer(memberId);
        BasketCaptainGrant grant = captainPermissionService.requireValidQualificationForDivision(
                memberId, null, division.getId());
        captainGrantSupport.assertGrantUnbound(grant);
        BasketTeam team = teamDao.selectById(teamPk);
        if (team == null || !NOT_DELETED.equals(team.getIsDelete())) {
            throw new ServiceException("球队不存在");
        }
        if (!Objects.equals(team.getCaptainMemberId(), memberId)) {
            throw new ServiceException("只有原球队队长可以继承这支球队", 403);
        }
        String reason = inheritBlockReason(team, division.getId(), memberId);
        if (reason != null) {
            throw new ServiceException(reason, 403);
        }
        if (Objects.equals(team.getDivisionId(), division.getId())) {
            throw new ServiceException("这支球队已经在本届赛事中", 403);
        }
        Long previousDivisionId = team.getDivisionId();
        membershipChangeGate.closeOpenActionsForPreviousEvent(team.getId(), previousDivisionId);
        closePreviousEventInvitesAndApplications(team.getId());
        moveRosterToDivision(team, division.getId(), memberId, playerProfile);
        team.setDivisionId(division.getId());
        if (Objects.equals(TeamStatusEnum.ARCHIVED.getCode(), team.getTeamStatus())) {
            team.setTeamStatus(TeamStatusEnum.ACTIVE.getCode());
        }
        team.setUpdateTime(new Date());
        teamDao.updateById(team);
        BasketTeamMember captainMember = loadCurrentMembership(memberId, team.getId());
        if (captainMember == null) {
            throw new ServiceException("队长成员关系不存在");
        }
        captainGrantSupport.assertGrantTeamDivisionMatch(grant, team);
        captainGrantSupport.bindGrantToTeam(grant, team.getId());
        updatePlayerCurrentTeam(playerProfile, team.getId());
        securityLogHelper.write(memberId, "TEAM_INHERIT", "TEAM_INHERIT",
                null, team.getTeamName(), memberId, clientIp);
        return toDetailResp(team, captainMember, resolveLogoUrl(team.getLogoFileId()), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamDetailResp create(TeamCreateReq req, String clientIp) {
        validateCreateReq(req);
        Long memberId = UserContext.getRequiredUserId();
        Long divisionId = req.getDivisionId();
        BasketDivision division = teamWritableSupport.requireDivisionForTeamCreate(divisionId);
        BasketPlayerProfile playerProfile = captainPermissionService.requireCertifiedPlayer(memberId);
        BasketCaptainGrant grant = captainPermissionService.requireValidQualificationForDivision(
                memberId, null, division.getId());
        captainGrantSupport.assertGrantUnbound(grant);
        if (!Objects.equals(grant.getDivisionId(), division.getId())) {
            throw new ServiceException(CaptainGrantSupport.GRANT_DIVISION_MISMATCH_MESSAGE, 403);
        }
        teamMembershipSupport.assertEligibleToJoinTeam(memberId, division.getId());
        assertTeamNameUnique(division.getId(), req.getTeamName().trim(), null);

        Date now = new Date();
        BasketTeam team = new BasketTeam();
        team.setTeamNo("TM" + IdUtil.getSnowflakeNextIdStr());
        team.setTeamName(req.getTeamName().trim());
        team.setShortName(shortNameOrName(req.getShortName(), req.getTeamName()));
        team.setGameTerminal("GENERAL");
        team.setRegionCode(trimToNull(req.getRegionCode()));
        team.setRegionName(trimToNull(req.getRegionName()));
        team.setDescription(trimToNull(req.getDescription()));
        team.setPublicStatus(req.getPublicStatus() == null
                ? TeamPublicStatusEnum.PRIVATE.getCode() : req.getPublicStatus());
        team.setCaptainMemberId(memberId);
        team.setCaptainPlayerId(playerProfile.getId());
        team.setContactMemberId(memberId);
        team.setDivisionId(division.getId());
        team.setMaxMemberCount(teamProperties.getDefaultMaxMemberCount());
        team.setCurrentMemberCount(1);
        team.setTeamStatus(TeamStatusEnum.ACTIVE.getCode());
        // 创建后为草稿，须显式提交认证后才进入 Admin「球队认证审核」
        team.setAuditStatus(TeamAuditStatusEnum.DRAFT.getCode());
        team.setEstablishedDate(now);
        team.setIsDelete(NOT_DELETED);

        if (StringUtils.hasText(req.getLogoFileId())) {
            Long logoFileId = parseLongId(req.getLogoFileId(), "Logo 文件主键");
            bindTeamLogoFile(team, logoFileId, memberId);
        }

        teamDao.insert(team);

        BasketTeamMember captainMember = new BasketTeamMember();
        captainMember.setMembershipNo("MB" + IdUtil.getSnowflakeNextIdStr());
        captainMember.setTeamId(team.getId());
        captainMember.setMemberId(memberId);
        captainMember.setPlayerId(playerProfile.getId());
        teamMembershipSupport.applyActiveMemberSlot(captainMember, division.getId(), memberId);
        captainMember.setRoleType(TeamMemberRoleEnum.CAPTAIN.getCode());
        captainMember.setRosterType(TeamRosterTypeEnum.OFFICIAL.getCode());
        captainMember.setJoinSource(TeamJoinSourceEnum.CREATE.getCode());
        captainMember.setMembershipStatus(TeamMembershipStatusEnum.ACTIVE.getCode());
        captainMember.setIsCurrent(IS_CURRENT);
        captainMember.setJoinTime(now);
        captainMember.setIsDelete(NOT_DELETED);
        teamMembershipSupport.insertMember(captainMember);

        captainGrantSupport.assertGrantTeamDivisionMatch(grant, team);
        captainGrantSupport.bindGrantToTeam(grant, team.getId());
        // current_team_id 仅作最近展示兼容字段，不作为成员关系事实来源
        updatePlayerCurrentTeam(playerProfile, team.getId());
        securityLogHelper.write(memberId, EVENT_TEAM_CREATE, "TEAM_CREATE",
                null, team.getTeamName(), memberId, clientIp);

        return toDetailResp(team, captainMember, resolveLogoUrl(team.getLogoFileId()), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamDetailResp createForPersonalFormation(Long drawTeamId, TeamCreateReq req, String clientIp) {
        Long memberId = UserContext.getRequiredUserId();
        BasketPersonalDrawTeam drawTeam = requirePersonalDrawCaptainTeam(drawTeamId, memberId);
        if (drawTeam.getConvertedRegistrationId() != null) {
            throw new ServiceException("该临时队伍已转换");
        }
        if (FormationStatusEnum.CONVERTED.getCode().equals(drawTeam.getFormationStatus())) {
            throw new ServiceException("该临时队伍已转换");
        }
        if (req != null && req.getDivisionId() == null) {
            req.setDivisionId(drawTeam.getDivisionId());
        }
        validateCreateReq(req);
        Long divisionId = req.getDivisionId();
        BasketDivision division = teamWritableSupport.requireDivisionForTeamCreate(divisionId);
        BasketPlayerProfile playerProfile = captainPermissionService.requireCertifiedPlayer(memberId);
        teamMembershipSupport.assertEligibleToJoinTeam(memberId, division.getId());
        assertTeamNameUnique(division.getId(), req.getTeamName().trim(), null);

        Date now = new Date();
        BasketTeam team = new BasketTeam();
        team.setTeamNo("TM" + IdUtil.getSnowflakeNextIdStr());
        team.setTeamName(req.getTeamName().trim());
        team.setShortName(shortNameOrName(req.getShortName(), req.getTeamName()));
        team.setGameTerminal("GENERAL");
        team.setRegionCode(trimToNull(req.getRegionCode()));
        team.setRegionName(trimToNull(req.getRegionName()));
        team.setDescription(trimToNull(req.getDescription()));
        team.setPublicStatus(req.getPublicStatus() == null
                ? TeamPublicStatusEnum.PRIVATE.getCode() : req.getPublicStatus());
        team.setCaptainMemberId(memberId);
        team.setCaptainPlayerId(playerProfile.getId());
        team.setContactMemberId(memberId);
        team.setDivisionId(division.getId());
        team.setMaxMemberCount(teamProperties.getDefaultMaxMemberCount());
        team.setCurrentMemberCount(1);
        team.setTeamStatus(TeamStatusEnum.ACTIVE.getCode());
        team.setAuditStatus(TeamAuditStatusEnum.APPROVED.getCode());
        team.setEstablishedDate(now);
        team.setIsDelete(NOT_DELETED);

        if (StringUtils.hasText(req.getLogoFileId())) {
            Long logoFileId = parseLongId(req.getLogoFileId(), "Logo 文件主键");
            bindTeamLogoFile(team, logoFileId, memberId);
        }

        teamDao.insert(team);

        BasketTeamMember captainMember = new BasketTeamMember();
        captainMember.setMembershipNo("MB" + IdUtil.getSnowflakeNextIdStr());
        captainMember.setTeamId(team.getId());
        captainMember.setMemberId(memberId);
        captainMember.setPlayerId(playerProfile.getId());
        teamMembershipSupport.applyActiveMemberSlot(captainMember, division.getId(), memberId);
        captainMember.setRoleType(TeamMemberRoleEnum.CAPTAIN.getCode());
        captainMember.setRosterType(TeamRosterTypeEnum.OFFICIAL.getCode());
        captainMember.setJoinSource(TeamJoinSourceEnum.CREATE.getCode());
        captainMember.setMembershipStatus(TeamMembershipStatusEnum.ACTIVE.getCode());
        captainMember.setIsCurrent(IS_CURRENT);
        captainMember.setJoinTime(now);
        captainMember.setIsDelete(NOT_DELETED);
        teamMembershipSupport.insertMember(captainMember);
        updatePlayerCurrentTeam(playerProfile, team.getId());
        securityLogHelper.write(memberId, EVENT_TEAM_CREATE, "personal-formation",
                null, team.getTeamName(), memberId, clientIp);
        return toDetailResp(team, captainMember, resolveLogoUrl(team.getLogoFileId()), true);
    }

    private BasketPersonalDrawTeam requirePersonalDrawCaptainTeam(Long drawTeamId, Long memberId) {
        if (drawTeamId == null) {
            throw new ServiceException("drawTeamId 不能为空");
        }
        BasketPersonalDrawTeam drawTeam = personalDrawTeamDao.selectById(drawTeamId);
        if (drawTeam == null || !NOT_DELETED.equals(drawTeam.getIsDelete())) {
            throw new ServiceException("组队任务不存在");
        }
        if (!Objects.equals(memberId, drawTeam.getCaptainUserId())) {
            throw new ServiceException("无权操作该组队任务", 403);
        }
        if (drawTeam.getCaptainPlayerId() == null) {
            throw new ServiceException("临时队伍尚未指定队长");
        }
        return drawTeam;
    }

    @Override
    public TeamDetailResp getDetail(String teamId) {
        Long teamPk = parseLongId(teamId, "球队主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTeam team = requireReadableTeam(teamPk, memberId);
        BasketTeamMember membership = loadAnyMembership(memberId, teamPk);
        EventContextMaps ctx = loadEventContextMaps(Collections.singletonList(team),
                membership == null || membership.getDivisionId() == null
                        ? Collections.emptySet() : Set.of(membership.getDivisionId()));
        boolean isCurrent = isCurrentTeamForUser(team, membership, ctx);
        boolean isCaptain = membership != null
                && TeamMemberRoleEnum.CAPTAIN.getCode().equals(membership.getRoleType());
        TeamDetailResp resp = toDetailResp(team, membership, resolveLogoUrl(team.getLogoFileId()), isCaptain);
        enrichTeamDetail(resp, team, membership, memberId, ctx,
                registrationStatus(loadRegistrationStatusMap(Collections.singletonList(team)), team.getId(),
                        displayDivisionId(team, membership)),
                !isCurrent);
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamDetailResp update(String teamId, TeamUpdateReq req, String clientIp) {
        if (req == null) {
            throw new ServiceException("请求不能为空");
        }
        Long teamPk = parseLongId(teamId, "球队主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTeam team = captainPermissionService.requireTeamCaptain(memberId, teamPk);
        teamWritableSupport.assertTeamWritable(team);
        String oldName = team.getTeamName();

        TeamAuditStatusEnum auditStatus = TeamAuditStatusEnum.fromCode(team.getAuditStatus());
        if (auditStatus.isApproved()) {
            if (hasAuditedFieldChange(req, team)) {
                throw new ServiceException("已认证球队名称、简称、介绍须提交资料变更审核");
            }
            if (req.getRegionCode() != null) {
                team.setRegionCode(trimToNull(req.getRegionCode()));
            }
            if (req.getRegionName() != null) {
                team.setRegionName(trimToNull(req.getRegionName()));
            }
            if (req.getPublicStatus() != null) {
                team.setPublicStatus(req.getPublicStatus());
            }
        } else if (auditStatus.isPendingAudit()) {
            throw new ServiceException("球队认证审核中，审核完成前暂不可修改球队认证资料");
        } else if (auditStatus.allowsDirectProfileEdit()) {
            // DRAFT / REJECTED：直接改待认证资料，不产生 ProfileChange，也不自动重提认证
            if (StringUtils.hasText(req.getTeamName()) && !req.getTeamName().trim().equals(team.getTeamName())) {
                assertTeamNameUnique(team.getDivisionId(), req.getTeamName().trim(), team.getId());
                team.setTeamName(req.getTeamName().trim());
            }
            if (StringUtils.hasText(req.getTeamName()) || req.getShortName() != null) {
                String newShortName = trimToNull(req.getShortName());
                if (newShortName == null) {
                    throw new ServiceException("球队简称不能为空");
                }
                if (newShortName.length() > 12) {
                    throw new ServiceException("球队简称长度应为 1-12 字");
                }
                team.setShortName(newShortName);
            }
            team.setGameTerminal(trimToNull(req.getGameTerminal()));
            team.setDescription(trimToNull(req.getDescription()));
            team.setRegionCode(trimToNull(req.getRegionCode()));
            team.setRegionName(trimToNull(req.getRegionName()));
            if (req.getPublicStatus() != null) {
                team.setPublicStatus(req.getPublicStatus());
            }
        } else {
            throw new ServiceException("当前认证状态不允许修改球队资料");
        }

        teamDao.updateById(team);
        securityLogHelper.write(memberId, EVENT_TEAM_UPDATE, "TEAM_UPDATE",
                oldName, team.getTeamName(), memberId, clientIp);

        BasketTeamMember membership = loadCurrentMembership(memberId, teamPk);
        return toDetailResp(team, membership, resolveLogoUrl(team.getLogoFileId()), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamDetailResp bindLogo(String teamId, TeamLogoBindReq req, String clientIp) {
        if (req == null || !StringUtils.hasText(req.getFileId())) {
            throw new ServiceException("Logo 文件主键不能为空");
        }
        Long teamPk = parseLongId(teamId, "球队主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTeam team = captainPermissionService.requireTeamCaptain(memberId, teamPk);
        teamWritableSupport.assertTeamWritable(team);
        TeamAuditStatusEnum auditStatus = TeamAuditStatusEnum.fromCode(team.getAuditStatus());
        if (auditStatus.isApproved()) {
            throw new ServiceException("已认证球队须通过资料变更审核修改 Logo");
        }
        if (auditStatus.isPendingAudit()) {
            throw new ServiceException("球队认证审核中，审核完成前暂不可修改球队认证资料");
        }
        if (!auditStatus.allowsDirectProfileEdit()) {
            throw new ServiceException("当前认证状态不允许修改球队 Logo");
        }

        Long fileId = parseLongId(req.getFileId(), "Logo 文件主键");
        bindTeamLogoFile(team, fileId, memberId);
        teamDao.updateById(team);
        securityLogHelper.write(memberId, EVENT_TEAM_LOGO, "TEAM_LOGO",
                null, team.getTeamName(), memberId, clientIp);

        BasketTeamMember membership = loadCurrentMembership(memberId, teamPk);
        return toDetailResp(team, membership, resolveLogoUrl(team.getLogoFileId()), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamDetailResp submitCertification(String teamId, String clientIp) {
        Long teamPk = parseLongId(teamId, "球队主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTeam team = captainPermissionService.requireTeamCaptain(memberId, teamPk);
        teamWritableSupport.assertTeamWritable(team);

        TeamAuditStatusEnum auditStatus = TeamAuditStatusEnum.fromCode(team.getAuditStatus());
        if (auditStatus.isApproved()) {
            throw new ServiceException("球队已完成认证，后续资料修改请提交资料变更审核");
        }
        if (auditStatus.isPendingAudit()) {
            throw new ServiceException("球队认证审核中，请勿重复提交");
        }
        if (!auditStatus.canSubmitCertification()) {
            throw new ServiceException("当前认证状态不允许提交认证");
        }
        if (!StringUtils.hasText(team.getTeamName())) {
            throw new ServiceException("请先填写球队名称后再提交认证");
        }

        boolean resubmit = auditStatus == TeamAuditStatusEnum.REJECTED;
        Integer before = team.getAuditStatus();
        Integer after = TeamAuditStatusEnum.PENDING.getCode();
        team.setAuditStatus(after);
        teamDao.updateById(team);

        writeCertificationSubmitRecord(team, memberId, before, after, resubmit);
        securityLogHelper.write(memberId, EVENT_TEAM_UPDATE,
                resubmit ? "TEAM_CERT_RESUBMIT" : "TEAM_CERT_SUBMIT",
                null, team.getTeamName(), memberId, clientIp);

        BasketTeamMember membership = loadCurrentMembership(memberId, teamPk);
        return toDetailResp(team, membership, resolveLogoUrl(team.getLogoFileId()), true);
    }

    private void bindTeamLogoFile(BasketTeam team, Long fileId, Long memberId) {
        BasketFileAsset asset = accountFileService.requireOwnedFile(fileId, memberId);
        if (!FileUsageTypeEnum.TEAM_LOGO.getCode().equalsIgnoreCase(asset.getUsageType())) {
            throw new ServiceException("???????");
        }
        if (!StringUtils.hasText(asset.getMimeType())
                || !asset.getMimeType().toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new ServiceException("?????????");
        }
        team.setLogoFileId(fileId);
    }

    private String resolvePageDisabledReason(boolean hasActiveTeam) {
        if (hasActiveTeam) {
            return "????????????????";
        }
        return null;
    }

    private Set<Long> loadPendingApplicationTeamIds(Long memberId) {
        Set<Long> teamIds = new HashSet<>();
        List<BasketTeamJoinApplication> pendingApps = joinApplicationDao.selectList(new LambdaQueryWrapper<BasketTeamJoinApplication>()
                .eq(BasketTeamJoinApplication::getApplicantMemberId, memberId)
                .eq(BasketTeamJoinApplication::getApplicationStatus, TeamJoinApplicationStatusEnum.PENDING.getCode())
                .eq(BasketTeamJoinApplication::getIsDelete, NOT_DELETED));
        for (BasketTeamJoinApplication application : pendingApps) {
            teamIds.add(application.getTeamId());
        }
        return teamIds;
    }

    private TeamSummaryResp toJoinableSummaryResp(BasketTeam team,
                                                  String logoUrl,
                                                  String captainName,
                                                  boolean hasActiveTeam,
                                                  Integer pendingApplicationStatus,
                                                  Set<Long> pendingTeamIds) {
        TeamSummaryResp resp = toSummaryResp(team, null, logoUrl);
        resp.setPlatformCode(null);
        resp.setRecruitStatus(team.getPublicStatus());
        resp.setMemberCount(team.getCurrentMemberCount());
        resp.setMaxMemberCount(team.getMaxMemberCount() != null ? team.getMaxMemberCount() : 8);
        resp.setCaptainName(captainName);
        resp.setDescription(team.getDescription());
        boolean hasPendingApply = pendingTeamIds.contains(team.getId());
        resp.setHasPendingApply(hasPendingApply);
        String disabledReason = resolveTeamApplyDisabledReason(team, hasActiveTeam,
                pendingApplicationStatus, pendingTeamIds);
        resp.setCanApply(disabledReason == null);
        resp.setAllowApply(disabledReason == null);
        resp.setCannotApplyReason(disabledReason);
        resp.setDisabledReason(disabledReason);
        return resp;
    }

    private String resolveTeamApplyDisabledReason(BasketTeam team,
                                                  boolean hasActiveInSameDivision,
                                                  Integer pendingApplicationStatus,
                                                  Set<Long> pendingTeamIds) {
        if (team.getDivisionId() == null) {
            return TeamWritableSupport.LEGACY_TEAM_READONLY_MESSAGE;
        }
        if (hasActiveInSameDivision) {
            return "当前账号已在该赛事加入球队，不能重复加入";
        }
        if (pendingTeamIds.contains(team.getId())) {
            return "已提交申请，请等待审核";
        }
        if (!Objects.equals(TeamAuditStatusEnum.APPROVED.getCode(), team.getAuditStatus())) {
            return "球队尚未通过认证审核";
        }
        if (!Objects.equals(TeamPublicStatusEnum.PUBLIC.getCode(), team.getPublicStatus())) {
            return "球队未开放招募";
        }
        int maxMembers = team.getMaxMemberCount() != null ? team.getMaxMemberCount() : 8;
        int currentMembers = team.getCurrentMemberCount() != null ? team.getCurrentMemberCount() : 0;
        if (currentMembers >= maxMembers) {
            return "球队人数已满";
        }
        if (!Objects.equals(TeamStatusEnum.ACTIVE.getCode(), team.getTeamStatus())) {
            return "球队当前不可加入";
        }
        return null;
    }

    private Long resolveInheritDivisionId(Long memberId, Long divisionId, boolean required) {
        if (divisionId != null) {
            BasketCaptainGrant grant = captainGrantSupport.findActiveGrantForDivision(memberId, divisionId);
            if (grant == null || grant.getTeamId() != null) {
                if (required) {
                    throw new ServiceException(grant == null
                            ? CaptainGrantSupport.NO_ACTIVE_GRANT_MESSAGE
                            : CaptainGrantSupport.GRANT_ALREADY_BOUND_MESSAGE, 403);
                }
                return null;
            }
            return divisionId;
        }
        List<BasketCaptainGrant> unbound = captainGrantSupport.findActiveGrantsForMember(memberId).stream()
                .filter(grant -> grant.getTeamId() == null && grant.getDivisionId() != null)
                .toList();
        if (unbound.size() == 1) {
            return unbound.get(0).getDivisionId();
        }
        if (required) {
            throw new ServiceException("请选择要继承到的赛事");
        }
        return null;
    }

    private String inheritBlockReason(BasketTeam team, Long targetDivisionId, Long captainMemberId) {
        if (team == null || !NOT_DELETED.equals(team.getIsDelete())) {
            return "球队不存在";
        }
        if (!Objects.equals(team.getCaptainMemberId(), captainMemberId)) {
            return "只有原球队队长可以继承这支球队";
        }
        if (!Objects.equals(TeamStatusEnum.ACTIVE.getCode(), team.getTeamStatus())
                && !Objects.equals(TeamStatusEnum.ARCHIVED.getCode(), team.getTeamStatus())) {
            return "这支球队已解散或停用，不能继承";
        }
        if (Objects.equals(team.getDivisionId(), targetDivisionId)) {
            return "这支球队已经在本届赛事中";
        }
        if (!sourceCompetitionClosed(team)) {
            return "这支球队所在赛事尚未结束，结束后才能继承到新赛事";
        }
        if (StringUtils.hasText(team.getTeamName())) {
            Long sameName = teamDao.selectCount(new LambdaQueryWrapper<BasketTeam>()
                    .eq(BasketTeam::getDivisionId, targetDivisionId)
                    .eq(BasketTeam::getTeamName, team.getTeamName())
                    .eq(BasketTeam::getIsDelete, NOT_DELETED)
                    .ne(BasketTeam::getId, team.getId()));
            if (sameName != null && sameName > 0) {
                return "本届赛事已有同名球队";
            }
        }
        BasketTeamMember occupied = teamMembershipSupport.findActiveMembership(captainMemberId, targetDivisionId);
        if (occupied != null && !Objects.equals(occupied.getTeamId(), team.getId())) {
            return "你已在本届赛事加入其他球队，请先离队后再继承原球队";
        }
        return null;
    }

    private boolean sourceCompetitionClosed(BasketTeam team) {
        if (team.getDivisionId() == null) {
            return true;
        }
        BasketDivision division = divisionDao.selectById(team.getDivisionId());
        if (division == null || !NOT_DELETED.equals(division.getIsDelete())) {
            return true;
        }
        BasketSeason season = null;
        if (division.getSeasonId() != null) {
            season = seasonDao.selectById(division.getSeasonId());
            if (season != null && !NOT_DELETED.equals(season.getIsDelete())) {
                season = null;
            }
        }
        return eventMatchPeriodEnded(division, season, new Date());
    }

    private String previousEventName(BasketTeam team) {
        if (team.getDivisionId() == null) {
            return "历史球队";
        }
        BasketDivision division = divisionDao.selectById(team.getDivisionId());
        if (division == null) {
            return "历史球队";
        }
        String seasonLabel = null;
        String seriesLabel = null;
        if (division.getSeasonId() != null) {
            BasketSeason season = seasonDao.selectById(division.getSeasonId());
            if (season != null) {
                seasonLabel = StringUtils.hasText(season.getDisplayTitle())
                        ? season.getDisplayTitle() : season.getSeasonName();
                if (season.getSeriesId() != null) {
                    BasketSeries series = seriesDao.selectById(season.getSeriesId());
                    if (series != null) {
                        seriesLabel = series.getSeriesName();
                    }
                }
            }
        }
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(seriesLabel)) {
            parts.add(seriesLabel);
        }
        if (StringUtils.hasText(seasonLabel)) {
            parts.add(seasonLabel);
        }
        if (StringUtils.hasText(division.getDivisionName())) {
            parts.add(division.getDivisionName());
        }
        return parts.isEmpty() ? "历史球队" : String.join(" · ", parts);
    }

    private int countTeamHonors(Long teamId) {
        if (teamId == null) {
            return 0;
        }
        Long count = honorRecordDao.selectCount(new LambdaQueryWrapper<BasketHonorRecord>()
                .eq(BasketHonorRecord::getTeamId, teamId)
                .eq(BasketHonorRecord::getIsDelete, NOT_DELETED));
        return count == null ? 0 : count.intValue();
    }

    private void closePreviousEventInvitesAndApplications(Long teamId) {
        Date now = new Date();
        invitationDao.update(null, new LambdaUpdateWrapper<BasketTeamInvitation>()
                .eq(BasketTeamInvitation::getTeamId, teamId)
                .in(BasketTeamInvitation::getInvitationStatus, java.util.List.of(
                        TeamInvitationStatusEnum.PENDING.getCode(),
                        TeamInvitationStatusEnum.AWAITING_AUDIT.getCode()))
                .eq(BasketTeamInvitation::getIsDelete, NOT_DELETED)
                .set(BasketTeamInvitation::getInvitationStatus, TeamInvitationStatusEnum.CANCELLED.getCode())
                .set(BasketTeamInvitation::getRespondedTime, now)
                .set(BasketTeamInvitation::getResponseRemark, "球队已沿用到新赛事，请重新邀请"));
        joinApplicationDao.update(null, new LambdaUpdateWrapper<com.zhs.model.BasketTeamJoinApplication>()
                .eq(com.zhs.model.BasketTeamJoinApplication::getTeamId, teamId)
                .eq(com.zhs.model.BasketTeamJoinApplication::getApplicationStatus, TeamJoinApplicationStatusEnum.PENDING.getCode())
                .eq(com.zhs.model.BasketTeamJoinApplication::getIsDelete, NOT_DELETED)
                .set(com.zhs.model.BasketTeamJoinApplication::getApplicationStatus, TeamJoinApplicationStatusEnum.CANCELLED.getCode())
                .set(com.zhs.model.BasketTeamJoinApplication::getProcessedTime, now)
                .set(com.zhs.model.BasketTeamJoinApplication::getProcessRemark, "球队已沿用到新赛事，请重新申请"));
    }

    /**
     * 旧赛季成员关系整届归档，队长原记录也留下。再给队长新建一条新赛季关系，新赛季名单里只有队长。
     */
    private void moveRosterToDivision(BasketTeam team, Long targetDivisionId, Long captainMemberId,
                                       BasketPlayerProfile captainProfile) {
        List<BasketTeamMember> members = teamMemberDao.selectList(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getTeamId, team.getId())
                .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                .eq(BasketTeamMember::getMembershipStatus, TeamMembershipStatusEnum.ACTIVE.getCode())
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED));
        for (BasketTeamMember member : members) {
            archiveRosterMemberForNewSeason(member);
            if (!Objects.equals(member.getMemberId(), captainMemberId)) {
                clearPlayerCurrentTeam(member.getPlayerId(), team.getId());
            }
        }
        Date now = new Date();
        BasketTeamMember captainMember = new BasketTeamMember();
        captainMember.setMembershipNo("MB" + IdUtil.getSnowflakeNextIdStr());
        captainMember.setTeamId(team.getId());
        captainMember.setMemberId(captainMemberId);
        captainMember.setPlayerId(captainProfile.getId());
        teamMembershipSupport.applyActiveMemberSlot(captainMember, targetDivisionId, captainMemberId);
        captainMember.setRoleType(TeamMemberRoleEnum.CAPTAIN.getCode());
        captainMember.setRosterType(TeamRosterTypeEnum.OFFICIAL.getCode());
        captainMember.setJoinSource(TeamJoinSourceEnum.CREATE.getCode());
        captainMember.setMembershipStatus(TeamMembershipStatusEnum.ACTIVE.getCode());
        captainMember.setIsCurrent(IS_CURRENT);
        captainMember.setJoinTime(now);
        captainMember.setIsDelete(NOT_DELETED);
        try {
            teamMembershipSupport.insertMember(captainMember);
        } catch (ServiceException ex) {
            throw new ServiceException("你已在本届赛事加入其他球队，请先离队后再继承原球队", 403);
        }
        team.setCurrentMemberCount(1);
    }

    private void clearPlayerCurrentTeam(Long playerId, Long teamId) {
        if (playerId == null || teamId == null) {
            return;
        }
        playerProfileDao.update(null, new LambdaUpdateWrapper<BasketPlayerProfile>()
                .eq(BasketPlayerProfile::getId, playerId)
                .eq(BasketPlayerProfile::getCurrentTeamId, teamId)
                .set(BasketPlayerProfile::getCurrentTeamId, null));
    }

    private void archiveRosterMemberForNewSeason(BasketTeamMember member) {
        Date now = new Date();
        member.setMembershipStatus(TeamMembershipStatusEnum.LEFT.getCode());
        member.setIsCurrent(0);
        member.setLeaveTime(now);
        member.setLeaveReason("DIVISION_FINISHED");
        member.setUpdateTime(now);
        teamMemberDao.updateById(member);
        teamMembershipSupport.clearActiveMemberSlot(member);
    }

    private void assertTeamNameUnique(Long divisionId, String teamName, Long excludeTeamId) {
        if (divisionId == null) {
            throw new ServiceException(TeamWritableSupport.LEGACY_TEAM_READONLY_MESSAGE, 403);
        }
        LambdaQueryWrapper<BasketTeam> wrapper = new LambdaQueryWrapper<BasketTeam>()
                .eq(BasketTeam::getDivisionId, divisionId)
                .eq(BasketTeam::getTeamName, teamName)
                .eq(BasketTeam::getIsDelete, NOT_DELETED);
        if (excludeTeamId != null) {
            wrapper.ne(BasketTeam::getId, excludeTeamId);
        }
        Long count = teamDao.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new ServiceException("该赛事下球队名称已存在");
        }
    }

    private BasketTeam requireReadableTeam(Long teamId, Long memberId) {
        BasketTeam team = teamDao.selectById(teamId);
        if (team == null || !NOT_DELETED.equals(team.getIsDelete())) {
            throw new ServiceException("球队不存在");
        }
        BasketTeamMember membership = loadAnyMembership(memberId, teamId);
        if (membership != null) {
            return team;
        }
        if (Objects.equals(TeamPublicStatusEnum.PUBLIC.getCode(), team.getPublicStatus())
                && Objects.equals(TeamStatusEnum.ACTIVE.getCode(), team.getTeamStatus())
                && team.getDivisionId() != null) {
            return team;
        }
        throw new ServiceException("无权查看该球队", 403);
    }

    private BasketTeamMember loadCurrentMembership(Long memberId, Long teamId) {
        return teamMemberDao.selectOne(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getTeamId, teamId)
                .eq(BasketTeamMember::getMemberId, memberId)
                .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                .eq(BasketTeamMember::getMembershipStatus, TeamMembershipStatusEnum.ACTIVE.getCode())
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED)
                .last("limit 1"));
    }

    private BasketTeamMember loadAnyMembership(Long memberId, Long teamId) {
        return teamMemberDao.selectOne(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getTeamId, teamId)
                .eq(BasketTeamMember::getMemberId, memberId)
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketTeamMember::getJoinTime)
                .orderByDesc(BasketTeamMember::getId)
                .last("limit 1"));
    }

    private Set<Long> loadJoinedTeamIds(Long memberId) {
        List<BasketTeamMember> memberships = teamMemberDao.selectList(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getMemberId, memberId)
                .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                .eq(BasketTeamMember::getMembershipStatus, TeamMembershipStatusEnum.ACTIVE.getCode())
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED));
        if (CollectionUtils.isEmpty(memberships)) {
            return Collections.emptySet();
        }
        return memberships.stream().map(BasketTeamMember::getTeamId).collect(Collectors.toSet());
    }

    private List<BasketTeam> loadTeams(Set<Long> teamIds) {
        if (CollectionUtils.isEmpty(teamIds)) {
            return Collections.emptyList();
        }
        return teamDao.selectByIds(teamIds).stream()
                .filter(team -> team != null && NOT_DELETED.equals(team.getIsDelete()))
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .collect(Collectors.toList());
    }

    private Map<Long, String> loadLogoUrlMap(List<BasketTeam> teams) {
        Set<Long> logoIds = teams.stream()
                .map(BasketTeam::getLogoFileId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (logoIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> map = new HashMap<>();
        for (BasketFileAsset asset : fileAssetDao.selectByIds(logoIds)) {
            if (asset != null && NOT_DELETED.equals(asset.getIsDelete())) {
                map.put(asset.getId(), asset.getAccessUrl());
            }
        }
        return map;
    }

    private String resolveLogoUrl(Long logoFileId) {
        if (logoFileId == null) {
            return null;
        }
        BasketFileAsset asset = fileAssetDao.selectById(logoFileId);
        if (asset == null || !NOT_DELETED.equals(asset.getIsDelete())) {
            return null;
        }
        return asset.getAccessUrl();
    }

    private void updatePlayerCurrentTeam(BasketPlayerProfile profile, Long teamId) {
        profile.setCurrentTeamId(teamId);
        playerProfileDao.updateById(profile);
    }

    private JoinableTeamPageResp emptyJoinablePage(long page, long size, String emptyReason) {
        JoinableTeamPageResp respPage = new JoinableTeamPageResp(page, size, 0);
        respPage.setRecords(Collections.emptyList());
        respPage.setEmptyReason(emptyReason);
        return respPage;
    }

    private JoinableTeamPageResp toJoinablePage(List<TeamSummaryResp> items, long page, long size) {
        long total = items.size();
        int from = (int) Math.min((page - 1) * size, total);
        int to = (int) Math.min(from + size, total);
        List<TeamSummaryResp> records = from >= to ? Collections.emptyList() : items.subList(from, to);
        JoinableTeamPageResp respPage = new JoinableTeamPageResp(page, size, total);
        respPage.setRecords(records);
        return respPage;
    }

    private TeamSummaryResp toSummaryResp(BasketTeam team, BasketTeamMember membership, String logoUrl) {
        TeamSummaryResp resp = new TeamSummaryResp();
        resp.setTeamId(toStringId(team.getId()));
        Long shownDivisionId = displayDivisionId(team, membership);
        resp.setDivisionId(shownDivisionId == null ? null : toStringId(shownDivisionId));
        resp.setTeamNo(team.getTeamNo());
        resp.setTeamName(team.getTeamName());
        resp.setShortName(team.getShortName());
        resp.setLogoUrl(logoUrl);
        resp.setRegionName(team.getRegionName());
        resp.setGameTerminal(team.getGameTerminal());
        resp.setPlatformCode(platformTeamMembershipSupport.normalizePlatformCode(team.getGameTerminal()));
        resp.setCurrentMemberCount(team.getCurrentMemberCount());
        resp.setMaxMemberCount(team.getMaxMemberCount());
        resp.setTeamStatus(team.getTeamStatus());
        resp.setAuditStatus(team.getAuditStatus());
        resp.setPublicStatus(team.getPublicStatus());
        resp.setCreateTime(team.getCreateTime());
        if (membership != null) {
            resp.setMyRole(membership.getRoleType());
            resp.setIsCaptain(TeamMemberRoleEnum.CAPTAIN.getCode().equals(membership.getRoleType()));
            resp.setMembershipStatus(membership.getMembershipStatus());
            resp.setLeaveReason(membership.getLeaveReason());
        } else {
            resp.setIsCaptain(false);
        }
        return resp;
    }

    private TeamDetailResp toDetailResp(BasketTeam team, BasketTeamMember membership, String logoUrl, boolean isCaptain) {
        TeamDetailResp resp = new TeamDetailResp();
        resp.setTeamId(toStringId(team.getId()));
        Long shownDivisionId = displayDivisionId(team, membership);
        resp.setDivisionId(shownDivisionId == null ? null : toStringId(shownDivisionId));
        resp.setTeamNo(team.getTeamNo());
        resp.setTeamName(team.getTeamName());
        resp.setShortName(team.getShortName());
        resp.setLogoFileId(toStringId(team.getLogoFileId()));
        resp.setLogoUrl(logoUrl);
        resp.setRegionCode(team.getRegionCode());
        resp.setRegionName(team.getRegionName());
        resp.setGameTerminal(team.getGameTerminal());
        resp.setDescription(team.getDescription());
        resp.setCurrentMemberCount(team.getCurrentMemberCount());
        resp.setMaxMemberCount(team.getMaxMemberCount());
        resp.setTeamStatus(team.getTeamStatus());
        resp.setAuditStatus(team.getAuditStatus());
        TeamAuditStatusEnum auditStatus = safeAuditStatus(team.getAuditStatus());
        resp.setAuditStatusText(auditStatus == null ? null : auditStatus.getDescription());
        if (auditStatus == TeamAuditStatusEnum.REJECTED) {
            resp.setCertificationRejectRemark(loadLatestCertificationRejectRemark(team.getId()));
        }
        resp.setPublicStatus(team.getPublicStatus());
        resp.setEstablishedDate(team.getEstablishedDate());
        resp.setCreateTime(team.getCreateTime());
        resp.setIsCaptain(isCaptain);
        resp.setMyRole(membership == null ? null : membership.getRoleType());
        if (membership != null) {
            resp.setMembershipStatus(membership.getMembershipStatus());
            resp.setLeaveReason(membership.getLeaveReason());
        }
        return resp;
    }

    private void enrichTeamSummary(TeamSummaryResp resp,
                                   BasketTeam team,
                                   BasketTeamMember membership,
                                   Long memberId,
                                   EventContextMaps ctx,
                                   String registrationStatus,
                                   boolean historical) {
        Long divisionId = displayDivisionId(team, membership);
        fillEventContext(resp, divisionId, ctx);
        resp.setRegistrationStatus(registrationStatus);
        resp.setIsHistorical(historical);
        resp.setCanManage(!historical && captainPermissionService.canManageTeam(memberId, team.getId()));
        resp.setDisplayStatusText(resolveDisplayStatusText(team, membership, ctx.divisionMap.get(divisionId),
                registrationStatus, historical, ctx.eventTeamSourceIds.contains(team.getId())));
    }

    private void enrichTeamDetail(TeamDetailResp resp,
                                  BasketTeam team,
                                  BasketTeamMember membership,
                                  Long memberId,
                                  EventContextMaps ctx,
                                  String registrationStatus,
                                  boolean historical) {
        Long divisionId = displayDivisionId(team, membership);
        BasketDivision division = divisionId == null ? null : ctx.divisionMap.get(divisionId);
        if (division != null) {
            resp.setDivisionName(division.getDivisionName());
            resp.setDivisionStatus(division.getDivisionStatus());
            resp.setDivisionPlatformCode(division.getPlatformCode());
            resp.setGameMode(division.getGameMode());
            BasketSeason season = division.getSeasonId() == null ? null : ctx.seasonMap.get(division.getSeasonId());
            if (season != null) {
                resp.setSeasonId(toStringId(season.getId()));
                resp.setSeasonName(firstNonBlank(season.getDisplayTitle(), season.getSeasonName()));
                BasketSeries series = season.getSeriesId() == null ? null : ctx.seriesMap.get(season.getSeriesId());
                if (series != null) {
                    resp.setSeriesId(toStringId(series.getId()));
                    resp.setSeriesName(series.getSeriesName());
                }
            }
        } else if (divisionId != null) {
            resp.setDivisionName("赛事信息不可用");
        } else {
            resp.setDivisionName("历史球队");
            resp.setSeriesName("赛事信息暂无");
        }
        if (historical && membership != null && membership.getDivisionId() != null
                && !Objects.equals(membership.getDivisionId(), team.getDivisionId())) {
            resp.setCurrentMemberCount(countDivisionMembers(team.getId(), membership.getDivisionId()));
        }
        resp.setRegistrationStatus(registrationStatus);
        resp.setIsHistorical(historical);
        resp.setCanManage(!historical && captainPermissionService.canManageTeam(memberId, team.getId()));
        resp.setDisplayStatusText(resolveDisplayStatusText(team, membership, division,
                registrationStatus, historical, ctx.eventTeamSourceIds.contains(team.getId())));
    }

    private void fillEventContext(TeamSummaryResp resp, BasketTeam team, EventContextMaps ctx) {
        fillEventContext(resp, team.getDivisionId(), ctx);
    }

    private void fillEventContext(TeamSummaryResp resp, Long divisionId, EventContextMaps ctx) {
        BasketDivision division = divisionId == null ? null : ctx.divisionMap.get(divisionId);
        if (division != null) {
            resp.setDivisionName(division.getDivisionName());
            resp.setDivisionStatus(division.getDivisionStatus());
            resp.setDivisionPlatformCode(division.getPlatformCode());
            resp.setGameMode(division.getGameMode());
            BasketSeason season = division.getSeasonId() == null ? null : ctx.seasonMap.get(division.getSeasonId());
            if (season != null) {
                resp.setSeasonId(toStringId(season.getId()));
                resp.setSeasonName(firstNonBlank(season.getDisplayTitle(), season.getSeasonName()));
                BasketSeries series = season.getSeriesId() == null ? null : ctx.seriesMap.get(season.getSeriesId());
                if (series != null) {
                    resp.setSeriesId(toStringId(series.getId()));
                    resp.setSeriesName(series.getSeriesName());
                }
            }
        } else if (divisionId != null) {
            resp.setDivisionName("赛事信息不可用");
        } else {
            resp.setDivisionName("历史球队");
            resp.setSeriesName("赛事信息暂无");
        }
    }

    /** 队员自己的届次优先。球队被继承到新赛季后，历史成员仍显示自己那一届。 */
    private Long displayDivisionId(BasketTeam team, BasketTeamMember membership) {
        if (membership != null && membership.getDivisionId() != null) {
            return membership.getDivisionId();
        }
        return team == null ? null : team.getDivisionId();
    }

    private Set<Long> membershipDivisionIds(Collection<BasketTeamMember> memberships) {
        if (memberships == null || memberships.isEmpty()) {
            return Collections.emptySet();
        }
        return memberships.stream()
                .map(BasketTeamMember::getDivisionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private int countDivisionMembers(Long teamId, Long divisionId) {
        Long count = teamMemberDao.selectCount(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getTeamId, teamId)
                .eq(BasketTeamMember::getDivisionId, divisionId)
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED));
        return count == null ? 0 : count.intValue();
    }

    private boolean isCurrentTeamForUser(BasketTeam team,
                                         BasketTeamMember membership,
                                         EventContextMaps ctx) {
        if (team == null || membership == null || ctx == null) {
            return false;
        }
        if (team.getDivisionId() == null || membership.getDivisionId() == null) {
            return false;
        }
        if (!Objects.equals(TeamStatusEnum.ACTIVE.getCode(), team.getTeamStatus())) {
            return false;
        }
        if (!Objects.equals(TeamMembershipStatusEnum.ACTIVE.getCode(), membership.getMembershipStatus())
                || !Objects.equals(IS_CURRENT, membership.getIsCurrent())) {
            return false;
        }
        if (!Objects.equals(membership.getDivisionId(), team.getDivisionId())) {
            return false;
        }
        if (!isDivisionOpenForCurrent(team.getDivisionId(), ctx.divisionMap)) {
            return false;
        }
        BasketDivision division = ctx.divisionMap.get(team.getDivisionId());
        BasketSeason season = division == null || division.getSeasonId() == null
                ? null : ctx.seasonMap.get(division.getSeasonId());
        return !eventMatchPeriodEnded(division, season, new Date());
    }

    private boolean isDivisionOpenForCurrent(Long divisionId, Map<Long, BasketDivision> divisionMap) {
        if (divisionId == null) {
            return false;
        }
        BasketDivision division = divisionMap.get(divisionId);
        if (division == null || !Objects.equals(NOT_DELETED, division.getIsDelete())) {
            return false;
        }
        String status = division.getDivisionStatus();
        return !DivisionStatusEnum.FINISHED.getCode().equals(status)
                && !DivisionStatusEnum.ARCHIVED.getCode().equals(status);
    }

    private EventContextMaps loadEventContextMaps(List<BasketTeam> teams) {
        return loadEventContextMaps(teams, Collections.emptySet());
    }

    private EventContextMaps loadEventContextMaps(List<BasketTeam> teams, Collection<Long> extraDivisionIds) {
        Set<Long> divisionIds = new HashSet<>();
        if (teams != null) {
            teams.stream()
                    .map(BasketTeam::getDivisionId)
                    .filter(Objects::nonNull)
                    .forEach(divisionIds::add);
        }
        if (extraDivisionIds != null) {
            extraDivisionIds.stream().filter(Objects::nonNull).forEach(divisionIds::add);
        }
        Map<Long, BasketDivision> divisionMap = new HashMap<>();
        if (!divisionIds.isEmpty()) {
            for (BasketDivision division : divisionDao.selectByIds(divisionIds)) {
                if (division != null && Objects.equals(NOT_DELETED, division.getIsDelete())) {
                    divisionMap.put(division.getId(), division);
                }
            }
        }
        Set<Long> seasonIds = divisionMap.values().stream()
                .map(BasketDivision::getSeasonId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, BasketSeason> seasonMap = new HashMap<>();
        if (!seasonIds.isEmpty()) {
            for (BasketSeason season : seasonDao.selectByIds(seasonIds)) {
                if (season != null && Objects.equals(NOT_DELETED, season.getIsDelete())) {
                    seasonMap.put(season.getId(), season);
                }
            }
        }
        Set<Long> seriesIds = seasonMap.values().stream()
                .map(BasketSeason::getSeriesId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, BasketSeries> seriesMap = new HashMap<>();
        if (!seriesIds.isEmpty()) {
            for (BasketSeries series : seriesDao.selectByIds(seriesIds)) {
                if (series != null && Objects.equals(NOT_DELETED, series.getIsDelete())) {
                    seriesMap.put(series.getId(), series);
                }
            }
        }
        Set<Long> teamIds = teams.stream().map(BasketTeam::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> eventTeamSourceIds = new HashSet<>();
        if (!teamIds.isEmpty()) {
            List<BasketEventTeam> eventTeams = eventTeamDao.selectList(new LambdaQueryWrapper<BasketEventTeam>()
                    .in(BasketEventTeam::getSourceTeamId, teamIds)
                    .eq(BasketEventTeam::getIsDelete, NOT_DELETED));
            for (BasketEventTeam eventTeam : eventTeams) {
                if (eventTeam.getSourceTeamId() != null) {
                    eventTeamSourceIds.add(eventTeam.getSourceTeamId());
                }
            }
        }
        return new EventContextMaps(divisionMap, seasonMap, seriesMap, eventTeamSourceIds);
    }

    private Map<Long, Map<Long, String>> loadRegistrationStatusMap(List<BasketTeam> teams) {
        Set<Long> teamIds = teams.stream().map(BasketTeam::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (teamIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BasketRegistration> registrations = registrationDao.selectList(new LambdaQueryWrapper<BasketRegistration>()
                .in(BasketRegistration::getTeamId, teamIds)
                .eq(BasketRegistration::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketRegistration::getUpdateTime)
                .orderByDesc(BasketRegistration::getId));
        Map<Long, Map<Long, String>> map = new HashMap<>();
        for (BasketRegistration registration : registrations) {
            if (registration.getTeamId() == null || registration.getDivisionId() == null) {
                continue;
            }
            map.computeIfAbsent(registration.getTeamId(), key -> new HashMap<>())
                    .putIfAbsent(registration.getDivisionId(), registration.getRegistrationStatus());
        }
        return map;
    }

    private String registrationStatus(Map<Long, Map<Long, String>> byTeam, Long teamId, Long divisionId) {
        if (byTeam == null || teamId == null || divisionId == null) {
            return null;
        }
        Map<Long, String> byDivision = byTeam.get(teamId);
        return byDivision == null ? null : byDivision.get(divisionId);
    }

    private String resolveDisplayStatusText(BasketTeam team,
                                            BasketTeamMember membership,
                                            BasketDivision division,
                                            String registrationStatus,
                                            boolean historical,
                                            boolean hasEventTeam) {
        if (historical) {
            if (team.getDivisionId() == null) {
                return "历史球队";
            }
            if (Objects.equals(TeamStatusEnum.DISSOLVED.getCode(), team.getTeamStatus())) {
                return "提前解散";
            }
            if (Objects.equals(TeamStatusEnum.DISABLED.getCode(), team.getTeamStatus())) {
                return "已停用";
            }
            if (membership != null && "DIVISION_FINISHED".equalsIgnoreCase(membership.getLeaveReason())) {
                return "赛事已结束";
            }
            if (Objects.equals(TeamStatusEnum.ARCHIVED.getCode(), team.getTeamStatus())
                    || (division != null && (DivisionStatusEnum.FINISHED.getCode().equals(division.getDivisionStatus())
                    || DivisionStatusEnum.ARCHIVED.getCode().equals(division.getDivisionStatus())))) {
                return "赛事已结束";
            }
            if (membership != null
                    && Objects.equals(TeamMembershipStatusEnum.REMOVED.getCode(), membership.getMembershipStatus())) {
                return "已离队";
            }
            if (membership != null
                    && !Objects.equals(TeamMembershipStatusEnum.ACTIVE.getCode(), membership.getMembershipStatus())) {
                return "已离队";
            }
            return "历史球队";
        }
        if (division != null && DivisionStatusEnum.PLAYING.getCode().equals(division.getDivisionStatus()) && hasEventTeam) {
            return "赛事进行中";
        }
        if (RegistrationStatusEnum.APPROVED.getCode().equals(registrationStatus) && hasEventTeam) {
            return "正式参赛";
        }
        if (RegistrationStatusEnum.APPROVED.getCode().equals(registrationStatus)) {
            return "正式参赛";
        }
        if (RegistrationStatusEnum.PENDING_REVIEW.getCode().equals(registrationStatus)) {
            return "报名审核中";
        }
        if (RegistrationStatusEnum.REJECTED.getCode().equals(registrationStatus)) {
            return "报名被驳回";
        }
        if (RegistrationStatusEnum.DRAFT.getCode().equals(registrationStatus)) {
            return "报名准备中";
        }
        return "组队中";
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

    private record EventContextMaps(Map<Long, BasketDivision> divisionMap,
                                    Map<Long, BasketSeason> seasonMap,
                                    Map<Long, BasketSeries> seriesMap,
                                    Set<Long> eventTeamSourceIds) {
    }

    /** 未传的字段不算修改。公开状态不在这里，已认证球队可直接改。 */
    private boolean hasAuditedFieldChange(TeamUpdateReq req, BasketTeam team) {
        if (req.getTeamName() != null
                && StringUtils.hasText(req.getTeamName())
                && !req.getTeamName().trim().equals(team.getTeamName())) {
            return true;
        }
        if (req.getShortName() != null
                && !Objects.equals(trimToNull(req.getShortName()), trimToNull(team.getShortName()))) {
            return true;
        }
        if (req.getDescription() != null
                && !Objects.equals(trimToNull(req.getDescription()), trimToNull(team.getDescription()))) {
            return true;
        }
        return false;
    }

    /**
     * 提交 / 重提认证留痕。认证挂在球队主记录上，不新增第二行申请；
     * Admin「球队认证审核」按 audit_status=PENDING/REVIEWING 查询，重提后即可看到。
     */
    private void writeCertificationSubmitRecord(BasketTeam team, Long operatorMemberId,
                                                Integer before, Integer after, boolean resubmit) {
        Date now = new Date();
        BasketTeamAuditRecord record = new BasketTeamAuditRecord();
        record.setTeamId(team.getId());
        record.setAuditType("TEAM_CREATE");
        record.setAuditAction(resubmit ? "RESUBMIT" : "SUBMIT");
        record.setBeforeStatus(before);
        record.setAfterStatus(after);
        record.setAuditorId(operatorMemberId);
        record.setAuditorName("队长");
        record.setAuditRemark(resubmit ? "队长修改资料后重新提交球队认证" : "队长提交球队认证");
        record.setAuditTime(now);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        record.setIsDelete(NOT_DELETED);
        teamAuditRecordDao.insert(record);
    }

    private String loadLatestCertificationRejectRemark(Long teamId) {
        if (teamId == null) {
            return null;
        }
        BasketTeamAuditRecord record = teamAuditRecordDao.selectOne(new LambdaQueryWrapper<BasketTeamAuditRecord>()
                .eq(BasketTeamAuditRecord::getTeamId, teamId)
                .eq(BasketTeamAuditRecord::getAuditAction, "REJECT")
                .eq(BasketTeamAuditRecord::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketTeamAuditRecord::getAuditTime)
                .orderByDesc(BasketTeamAuditRecord::getId)
                .last("limit 1"));
        return record == null ? null : trimToNull(record.getAuditRemark());
    }

    private TeamAuditStatusEnum safeAuditStatus(Integer code) {
        if (code == null) {
            return null;
        }
        try {
            return TeamAuditStatusEnum.fromCode(code);
        } catch (ServiceException ex) {
            return null;
        }
    }

    private void validateCreateReq(TeamCreateReq req) {
        if (req == null || !StringUtils.hasText(req.getTeamName())) {
            throw new ServiceException("球队名称不能为空");
        }
        if (req.getDivisionId() == null) {
            throw new ServiceException("divisionId 不能为空");
        }
        if (req.getTeamName().trim().length() > 50) {
            throw new ServiceException("球队名称过长");
        }
    }

    private IPage<TeamSummaryResp> emptyPage(Integer currentPage, Integer pageSize) {
        return toPage(Collections.emptyList(), currentPage, pageSize);
    }

    private IPage<TeamSummaryResp> toPage(List<TeamSummaryResp> items, Integer currentPage, Integer pageSize) {
        long page = normalizePage(currentPage);
        long size = normalizeSize(pageSize);
        long total = items.size();
        int from = (int) Math.min((page - 1) * size, total);
        int to = (int) Math.min(from + size, total);
        List<TeamSummaryResp> records = from >= to ? Collections.emptyList() : items.subList(from, to);
        Page<TeamSummaryResp> respPage = new Page<>(page, size, total);
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
            throw new ServiceException(fieldName + "????");
        }
        try {
            return Long.parseLong(id.trim());
        } catch (NumberFormatException ex) {
            throw new ServiceException(fieldName + "?????");
        }
    }

    private String toStringId(Long id) {
        return id == null ? null : String.valueOf(id);
    }

    private String shortNameOrName(String shortName, String teamName) {
        String value = trimToNull(shortName);
        if (value != null) {
            return value.length() > 32 ? value.substring(0, 32) : value;
        }
        String name = teamName == null ? "" : teamName.trim();
        return name.length() > 32 ? name.substring(0, 32) : name;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
