package com.zhs.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.common.UserContext;
import com.zhs.config.AccountTeamProperties;
import com.zhs.dao.BasketFileAssetDao;
import com.zhs.dao.BasketMemberProfileDao;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.BasketTeamDao;
import com.zhs.dao.BasketTeamJoinApplicationDao;
import com.zhs.dao.BasketPlayerPlatformAccountDao;
import com.zhs.dao.BasketTeamMemberDao;
import com.zhs.enums.FileUsageTypeEnum;
import com.zhs.enums.PlatformIdentityStatusEnum;
import com.zhs.enums.TeamAuditStatusEnum;
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
import com.zhs.model.BasketPlayerPlatformAccount;
import com.zhs.model.BasketMemberProfile;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
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
    private BasketTeamMemberDao teamMemberDao;
    @Resource
    private BasketTeamJoinApplicationDao joinApplicationDao;
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
        EventContextMaps ctx = loadEventContextMaps(teams);
        Map<Long, String> logoUrlMap = loadLogoUrlMap(teams);
        Map<Long, String> registrationStatusMap = loadRegistrationStatusMap(teams);

        List<TeamSummaryResp> items = new ArrayList<>();
        for (Map.Entry<Long, BasketTeamMember> entry : latestByTeam.entrySet()) {
            BasketTeam team = teamMap.get(entry.getKey());
            if (team == null) {
                continue;
            }
            boolean isCurrentTeam = isCurrentTeamForUser(team, entry.getValue(), ctx.divisionMap);
            if (history == isCurrentTeam) {
                continue;
            }
            TeamSummaryResp resp = toSummaryResp(team, entry.getValue(), logoUrlMap.get(team.getLogoFileId()));
            enrichTeamSummary(resp, team, entry.getValue(), memberId, ctx, registrationStatusMap.get(team.getId()), !isCurrentTeam);
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

        List<BasketDivision> openDivisions = divisionDao.selectList(new LambdaQueryWrapper<BasketDivision>()
                .eq(BasketDivision::getIsDelete, NOT_DELETED)
                .notIn(BasketDivision::getDivisionStatus,
                        DivisionStatusEnum.FINISHED.getCode(),
                        DivisionStatusEnum.ARCHIVED.getCode()));
        if (CollectionUtils.isEmpty(openDivisions)) {
            return fillJoinablePageMeta(
                    emptyJoinablePage(page, size, "暂无可加入的赛事球队"),
                    hasActiveTeam, currentTeam, pendingApplicationStatus);
        }
        Set<Long> openDivisionIds = openDivisions.stream()
                .map(BasketDivision::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        LambdaQueryWrapper<BasketTeam> wrapper = new LambdaQueryWrapper<BasketTeam>()
                .eq(BasketTeam::getIsDelete, NOT_DELETED)
                .eq(BasketTeam::getTeamStatus, TeamStatusEnum.ACTIVE.getCode())
                .eq(BasketTeam::getAuditStatus, TeamAuditStatusEnum.APPROVED.getCode())
                .eq(BasketTeam::getPublicStatus, TeamPublicStatusEnum.PUBLIC.getCode())
                .in(BasketTeam::getDivisionId, openDivisionIds)
                .orderByDesc(BasketTeam::getCreateTime)
                .orderByDesc(BasketTeam::getId);
        if (!joinedTeamIds.isEmpty()) {
            wrapper.notIn(BasketTeam::getId, joinedTeamIds);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(BasketTeam::getTeamName, kw).or().like(BasketTeam::getShortName, kw));
        }

        IPage<BasketTeam> teamPage = teamDao.selectPage(new Page<>(page, size), wrapper);
        List<BasketTeam> pageTeams = teamPage.getRecords() == null ? Collections.emptyList() : teamPage.getRecords();
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

        JoinableTeamPageResp respPage = new JoinableTeamPageResp(teamPage.getCurrent(), teamPage.getSize(), teamPage.getTotal());
        respPage.setRecords(items);
        if (teamPage.getTotal() <= 0) {
            respPage.setEmptyReason("暂无可加入的赛事球队");
        }
        return fillJoinablePageMeta(respPage, hasActiveTeam, currentTeam, pendingApplicationStatus);
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
    public List<InheritableTeamSummaryResp> listInheritableTeams() {
        // 历史球队复活已废弃：始终返回空列表，兼容旧前端
        return Collections.emptyList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamDetailResp bindGrantToTeam(String teamId) {
        throw new ServiceException("不再支持将队长授权绑定到历史球队，请在当前赛事重新创建球队", 403);
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
        team.setShortName(trimToNull(req.getShortName()));
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
        team.setAuditStatus(TeamAuditStatusEnum.PENDING.getCode());
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
        team.setShortName(trimToNull(req.getShortName()));
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
        EventContextMaps ctx = loadEventContextMaps(Collections.singletonList(team));
        boolean isCurrent = isCurrentTeamForUser(team, membership, ctx.divisionMap);
        boolean isCaptain = membership != null
                && TeamMemberRoleEnum.CAPTAIN.getCode().equals(membership.getRoleType());
        TeamDetailResp resp = toDetailResp(team, membership, resolveLogoUrl(team.getLogoFileId()), isCaptain);
        enrichTeamDetail(resp, team, membership, memberId, ctx, loadRegistrationStatusMap(Collections.singletonList(team)).get(team.getId()), !isCurrent);
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
        if (auditStatus == TeamAuditStatusEnum.APPROVED) {
            if (hasAuditedFieldChange(req, team)) {
                throw new ServiceException("已认证球队名称、简称、介绍须提交资料变更审核");
            }
            team.setRegionCode(trimToNull(req.getRegionCode()));
            team.setRegionName(trimToNull(req.getRegionName()));
            if (req.getPublicStatus() != null) {
                team.setPublicStatus(req.getPublicStatus());
            }
        } else {
            if (StringUtils.hasText(req.getTeamName()) && !req.getTeamName().trim().equals(team.getTeamName())) {
                assertTeamNameUnique(team.getDivisionId(), req.getTeamName().trim(), team.getId());
                team.setTeamName(req.getTeamName().trim());
            }
            team.setShortName(trimToNull(req.getShortName()));
            team.setGameTerminal(trimToNull(req.getGameTerminal()));
            team.setDescription(trimToNull(req.getDescription()));
            team.setRegionCode(trimToNull(req.getRegionCode()));
            team.setRegionName(trimToNull(req.getRegionName()));
            if (req.getPublicStatus() != null) {
                team.setPublicStatus(req.getPublicStatus());
            }
        }

        teamDao.updateById(team);
        securityLogHelper.write(memberId, EVENT_TEAM_UPDATE, "??????",
                oldName, team.getTeamName(), memberId, clientIp);

        BasketTeamMember membership = loadCurrentMembership(memberId, teamPk);
        return toDetailResp(team, membership, resolveLogoUrl(team.getLogoFileId()), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamDetailResp bindLogo(String teamId, TeamLogoBindReq req, String clientIp) {
        if (req == null || !StringUtils.hasText(req.getFileId())) {
            throw new ServiceException("????????");
        }
        Long teamPk = parseLongId(teamId, "????");
        Long memberId = UserContext.getRequiredUserId();
        BasketTeam team = captainPermissionService.requireTeamCaptain(memberId, teamPk);
        teamWritableSupport.assertTeamWritable(team);
        if (Objects.equals(TeamAuditStatusEnum.APPROVED.getCode(), team.getAuditStatus())) {
            throw new ServiceException("已认证球队须通过资料变更审核修改 Logo");
        }

        Long fileId = parseLongId(req.getFileId(), "????");
        bindTeamLogoFile(team, fileId, memberId);
        teamDao.updateById(team);
        securityLogHelper.write(memberId, EVENT_TEAM_LOGO, "???? Logo",
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
        resp.setDivisionId(team.getDivisionId() == null ? null : toStringId(team.getDivisionId()));
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
        resp.setDivisionId(team.getDivisionId() == null ? null : toStringId(team.getDivisionId()));
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
        fillEventContext(resp, team, ctx);
        resp.setRegistrationStatus(registrationStatus);
        resp.setIsHistorical(historical);
        resp.setCanManage(!historical && captainPermissionService.canManageTeam(memberId, team.getId()));
        resp.setDisplayStatusText(resolveDisplayStatusText(team, membership, ctx.divisionMap.get(team.getDivisionId()),
                registrationStatus, historical, ctx.eventTeamSourceIds.contains(team.getId())));
    }

    private void enrichTeamDetail(TeamDetailResp resp,
                                  BasketTeam team,
                                  BasketTeamMember membership,
                                  Long memberId,
                                  EventContextMaps ctx,
                                  String registrationStatus,
                                  boolean historical) {
        BasketDivision division = team.getDivisionId() == null ? null : ctx.divisionMap.get(team.getDivisionId());
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
        } else if (team.getDivisionId() != null) {
            resp.setDivisionName("赛事信息不可用");
        } else {
            resp.setDivisionName("历史球队");
            resp.setSeriesName("赛事信息暂无");
        }
        resp.setRegistrationStatus(registrationStatus);
        resp.setIsHistorical(historical);
        resp.setCanManage(!historical && captainPermissionService.canManageTeam(memberId, team.getId()));
        resp.setDisplayStatusText(resolveDisplayStatusText(team, membership, division,
                registrationStatus, historical, ctx.eventTeamSourceIds.contains(team.getId())));
    }

    private void fillEventContext(TeamSummaryResp resp, BasketTeam team, EventContextMaps ctx) {
        BasketDivision division = team.getDivisionId() == null ? null : ctx.divisionMap.get(team.getDivisionId());
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
        } else if (team.getDivisionId() != null) {
            resp.setDivisionName("赛事信息不可用");
        } else {
            resp.setDivisionName("历史球队");
            resp.setSeriesName("赛事信息暂无");
        }
    }

    private boolean isCurrentTeamForUser(BasketTeam team,
                                         BasketTeamMember membership,
                                         Map<Long, BasketDivision> divisionMap) {
        if (team == null || membership == null) {
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
        return isDivisionOpenForCurrent(team.getDivisionId(), divisionMap);
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
        Set<Long> divisionIds = teams.stream()
                .map(BasketTeam::getDivisionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
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

    private Map<Long, String> loadRegistrationStatusMap(List<BasketTeam> teams) {
        Set<Long> teamIds = teams.stream().map(BasketTeam::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (teamIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BasketRegistration> registrations = registrationDao.selectList(new LambdaQueryWrapper<BasketRegistration>()
                .in(BasketRegistration::getTeamId, teamIds)
                .eq(BasketRegistration::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketRegistration::getUpdateTime)
                .orderByDesc(BasketRegistration::getId));
        Map<Long, String> map = new HashMap<>();
        for (BasketRegistration registration : registrations) {
            if (registration.getTeamId() != null) {
                map.putIfAbsent(registration.getTeamId(), registration.getRegistrationStatus());
            }
        }
        return map;
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

    private boolean hasAuditedFieldChange(TeamUpdateReq req, BasketTeam team) {
        if (StringUtils.hasText(req.getTeamName())
                && !req.getTeamName().trim().equals(team.getTeamName())) {
            return true;
        }
        String newShortName = trimToNull(req.getShortName());
        if (!Objects.equals(newShortName, trimToNull(team.getShortName()))) {
            return true;
        }
        String newDescription = trimToNull(req.getDescription());
        return !Objects.equals(newDescription, trimToNull(team.getDescription()));
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

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
