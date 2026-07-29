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
import com.zhs.model.BasketCaptainQualification;
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

    @Override
    public IPage<TeamSummaryResp> pageMine(Integer currentPage, Integer pageSize) {
        Long memberId = UserContext.getRequiredUserId();
        List<BasketTeamMember> memberships = teamMemberDao.selectList(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getMemberId, memberId)
                .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                .eq(BasketTeamMember::getMembershipStatus, TeamMembershipStatusEnum.ACTIVE.getCode())
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketTeamMember::getJoinTime)
                .orderByDesc(BasketTeamMember::getId));
        if (CollectionUtils.isEmpty(memberships)) {
            return emptyPage(currentPage, pageSize);
        }

        Map<Long, BasketTeamMember> membershipMap = memberships.stream()
                .collect(Collectors.toMap(BasketTeamMember::getTeamId, m -> m, (a, b) -> a));
        List<BasketTeam> teams = loadTeams(membershipMap.keySet());
        Map<Long, String> logoUrlMap = loadLogoUrlMap(teams);

        List<TeamSummaryResp> items = new ArrayList<>();
        for (BasketTeam team : teams) {
            BasketTeamMember membership = membershipMap.get(team.getId());
            items.add(toSummaryResp(team, membership, logoUrlMap.get(team.getLogoFileId())));
        }
        return toPage(items, currentPage, pageSize);
    }

    @Override
    public JoinableTeamPageResp pageJoinable(Integer currentPage, Integer pageSize, String platformCode, String keyword) {
        Long memberId = UserContext.getRequiredUserId();
        long page = normalizePage(currentPage);
        long size = normalizeSize(pageSize);

        BasketTeamMember activeMembership = teamMembershipSupport.findActiveMembership(memberId);
        BasketTeam currentTeam = activeMembership == null ? null : teamDao.selectById(activeMembership.getTeamId());
        Integer pendingApplicationStatus = teamMembershipSupport.findPendingApplicationStatus(memberId);
        boolean hasActiveTeam = activeMembership != null;
        String pageDisabledReason = resolvePageDisabledReason(hasActiveTeam);

        captainPermissionService.requireCertifiedPlayer(memberId);

        LambdaQueryWrapper<BasketTeam> wrapper = new LambdaQueryWrapper<BasketTeam>()
                .eq(BasketTeam::getIsDelete, NOT_DELETED)
                .eq(BasketTeam::getTeamStatus, TeamStatusEnum.ACTIVE.getCode())
                .eq(BasketTeam::getAuditStatus, TeamAuditStatusEnum.APPROVED.getCode())
                .eq(BasketTeam::getPublicStatus, TeamPublicStatusEnum.PUBLIC.getCode())
                .orderByDesc(BasketTeam::getCreateTime)
                .orderByDesc(BasketTeam::getId);
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(BasketTeam::getTeamName, kw).or().like(BasketTeam::getShortName, kw));
        }

        List<BasketTeam> allTeams = teamDao.selectList(wrapper);
        Set<Long> pendingTeamIds = loadPendingApplicationTeamIds(memberId);
        List<BasketTeam> joinable = allTeams.stream()
                .filter(team -> activeMembership == null || !activeMembership.getTeamId().equals(team.getId()))
                .collect(Collectors.toList());

        Map<Long, String> logoUrlMap = loadLogoUrlMap(joinable);
        Map<Long, String> captainNameMap = loadCaptainNameMap(joinable);
        List<TeamSummaryResp> items = joinable.stream()
                .map(team -> toJoinableSummaryResp(team, logoUrlMap.get(team.getLogoFileId()),
                        captainNameMap.get(team.getCaptainMemberId()),
                        hasActiveTeam, pendingApplicationStatus, pendingTeamIds))
                .collect(Collectors.toList());

        JoinableTeamPageResp respPage = toJoinablePage(items, page, size);
        respPage.setPlatformCode(null);
        respPage.setPlatformIdentityStatus(null);
        respPage.setHasActiveTeam(hasActiveTeam);
        respPage.setCurrentTeamId(currentTeam == null ? null : toStringId(currentTeam.getId()));
        respPage.setCurrentTeamName(currentTeam == null ? null : currentTeam.getTeamName());
        respPage.setPendingApplicationStatus(pendingApplicationStatus);
        respPage.setCanApply(pageDisabledReason == null);
        respPage.setDisabledReason(pageDisabledReason);
        if (items.isEmpty()) {
            respPage.setEmptyReason(hasActiveTeam ? "?????????" : "????????????");
        }
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
        Long memberId = UserContext.getRequiredUserId();
        BasketCaptainQualification grant = captainGrantSupport.findCurrentGrant(memberId);
        if (grant == null || grant.getTeamId() != null) {
            return Collections.emptyList();
        }
        try {
            captainPermissionService.requireValidQualification(memberId);
        } catch (ServiceException ex) {
            return Collections.emptyList();
        }

        List<BasketTeam> historicalTeams = teamDao.selectList(new LambdaQueryWrapper<BasketTeam>()
                .eq(BasketTeam::getCaptainMemberId, memberId)
                .eq(BasketTeam::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketTeam::getCreateTime)
                .orderByDesc(BasketTeam::getId));
        if (CollectionUtils.isEmpty(historicalTeams)) {
            return Collections.emptyList();
        }

        Map<Long, String> logoUrlMap = loadLogoUrlMap(historicalTeams);
        List<InheritableTeamSummaryResp> items = new ArrayList<>();
        for (BasketTeam team : historicalTeams) {
            InheritableTeamSummaryResp resp = new InheritableTeamSummaryResp();
            resp.setTeamId(toStringId(team.getId()));
            resp.setTeamNo(team.getTeamNo());
            resp.setTeamName(team.getTeamName());
            resp.setShortName(team.getShortName());
            resp.setLogoUrl(logoUrlMap.get(team.getLogoFileId()));
            resp.setAuditStatus(team.getAuditStatus());
            resp.setAuditStatusText(TeamAuditStatusEnum.fromCode(team.getAuditStatus()).getDescription());
            resp.setCurrentMemberCount(team.getCurrentMemberCount());
            String cannotInheritReason = resolveCannotInheritReason(team);
            resp.setCanInherit(cannotInheritReason == null);
            resp.setCannotInheritReason(cannotInheritReason);
            items.add(resp);
        }
        return items;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamDetailResp bindGrantToTeam(String teamId) {
        Long memberId = UserContext.getRequiredUserId();
        BasketCaptainQualification grant = captainPermissionService.requireValidQualification(memberId);
        if (grant.getTeamId() != null) {
            throw new ServiceException("?????????????????????", 403);
        }
        Long teamPk = parseLongId(teamId, "????");
        BasketTeam team = teamDao.selectById(teamPk);
        if (team == null || !NOT_DELETED.equals(team.getIsDelete())) {
            throw new ServiceException("?????");
        }
        String cannotInheritReason = resolveCannotInheritReason(team);
        if (cannotInheritReason != null) {
            throw new ServiceException(cannotInheritReason, 403);
        }
        if (!Objects.equals(memberId, team.getCaptainMemberId())) {
            throw new ServiceException("???????????????", 403);
        }
        captainGrantSupport.bindGrantToTeam(memberId, teamPk);
        BasketTeamMember membership = loadCurrentMembership(memberId, teamPk);
        return toDetailResp(team, membership, resolveLogoUrl(team.getLogoFileId()), true);
    }

    private String resolveCannotInheritReason(BasketTeam team) {
        if (!Objects.equals(TeamStatusEnum.ACTIVE.getCode(), team.getTeamStatus())) {
            return "??????????????";
        }
        TeamAuditStatusEnum auditStatus = TeamAuditStatusEnum.fromCode(team.getAuditStatus());
        if (auditStatus == TeamAuditStatusEnum.PENDING || auditStatus == TeamAuditStatusEnum.REVIEWING) {
            return "???????????";
        }
        if (auditStatus == TeamAuditStatusEnum.REJECTED) {
            return "????????????";
        }
        if (auditStatus != TeamAuditStatusEnum.APPROVED) {
            return "????????????";
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamDetailResp create(TeamCreateReq req, String clientIp) {
        validateCreateReq(req);
        Long memberId = UserContext.getRequiredUserId();
        BasketCaptainQualification qualification = captainPermissionService.requireValidQualification(memberId);
        captainGrantSupport.assertUnboundForCreate(qualification);
        BasketPlayerProfile playerProfile = captainPermissionService.requireCertifiedPlayer(memberId);
        teamMembershipSupport.assertEligibleToJoinTeam(memberId);

        assertTeamNameUnique(req.getTeamName().trim());

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
        team.setMaxMemberCount(teamProperties.getDefaultMaxMemberCount());
        team.setCurrentMemberCount(1);
        team.setTeamStatus(TeamStatusEnum.ACTIVE.getCode());
        team.setAuditStatus(TeamAuditStatusEnum.PENDING.getCode());
        team.setEstablishedDate(now);
        team.setIsDelete(NOT_DELETED);

        if (StringUtils.hasText(req.getLogoFileId())) {
            Long logoFileId = parseLongId(req.getLogoFileId(), "Logo ????");
            bindTeamLogoFile(team, logoFileId, memberId);
        }

        teamDao.insert(team);

        BasketTeamMember captainMember = new BasketTeamMember();
        captainMember.setMembershipNo("MB" + IdUtil.getSnowflakeNextIdStr());
        captainMember.setTeamId(team.getId());
        captainMember.setMemberId(memberId);
        captainMember.setPlayerId(playerProfile.getId());
        teamMembershipSupport.applyActiveMemberSlot(captainMember, memberId);
        captainMember.setRoleType(TeamMemberRoleEnum.CAPTAIN.getCode());
        captainMember.setRosterType(TeamRosterTypeEnum.OFFICIAL.getCode());
        captainMember.setJoinSource(TeamJoinSourceEnum.CREATE.getCode());
        captainMember.setMembershipStatus(TeamMembershipStatusEnum.ACTIVE.getCode());
        captainMember.setIsCurrent(IS_CURRENT);
        captainMember.setJoinTime(now);
        captainMember.setIsDelete(NOT_DELETED);
        teamMembershipSupport.insertMember(captainMember);

        captainGrantSupport.bindGrantToTeam(memberId, team.getId());
        updatePlayerCurrentTeam(playerProfile, team.getId());
        securityLogHelper.write(memberId, EVENT_TEAM_CREATE, "????",
                null, team.getTeamName(), memberId, clientIp);

        BasketTeamMember membership = captainMember;
        return toDetailResp(team, membership, resolveLogoUrl(team.getLogoFileId()), true);
    }

    @Override
    public TeamDetailResp getDetail(String teamId) {
        Long teamPk = parseLongId(teamId, "????");
        Long memberId = UserContext.getRequiredUserId();
        BasketTeam team = requireReadableTeam(teamPk, memberId);
        BasketTeamMember membership = loadCurrentMembership(memberId, teamPk);
        return toDetailResp(team, membership, resolveLogoUrl(team.getLogoFileId()),
                membership != null && TeamMemberRoleEnum.CAPTAIN.getCode().equals(membership.getRoleType()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamDetailResp update(String teamId, TeamUpdateReq req, String clientIp) {
        if (req == null) {
            throw new ServiceException("????????");
        }
        Long teamPk = parseLongId(teamId, "????");
        Long memberId = UserContext.getRequiredUserId();
        BasketTeam team = captainPermissionService.requireTeamCaptain(memberId, teamPk);
        String oldName = team.getTeamName();

        TeamAuditStatusEnum auditStatus = TeamAuditStatusEnum.fromCode(team.getAuditStatus());
        if (auditStatus == TeamAuditStatusEnum.APPROVED) {
            team.setDescription(trimToNull(req.getDescription()));
            team.setRegionCode(trimToNull(req.getRegionCode()));
            team.setRegionName(trimToNull(req.getRegionName()));
            if (req.getPublicStatus() != null) {
                team.setPublicStatus(req.getPublicStatus());
            }
        } else {
            if (StringUtils.hasText(req.getTeamName()) && !req.getTeamName().trim().equals(team.getTeamName())) {
                assertTeamNameUnique(req.getTeamName().trim());
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
        if (Objects.equals(TeamAuditStatusEnum.APPROVED.getCode(), team.getAuditStatus())) {
            throw new ServiceException("???????????? Logo");
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
                                                  boolean hasActiveTeam,
                                                  Integer pendingApplicationStatus,
                                                  Set<Long> pendingTeamIds) {
        if (hasActiveTeam) {
            return "????????????????";
        }
        if (pendingTeamIds.contains(team.getId())) {
            return "?????";
        }
        if (pendingApplicationStatus != null) {
            return "????????????";
        }
        if (!Objects.equals(TeamAuditStatusEnum.APPROVED.getCode(), team.getAuditStatus())) {
            return "?????????????";
        }
        if (!Objects.equals(TeamPublicStatusEnum.PUBLIC.getCode(), team.getPublicStatus())) {
            return "???????";
        }
        int maxMembers = team.getMaxMemberCount() != null ? team.getMaxMemberCount() : 8;
        int currentMembers = team.getCurrentMemberCount() != null ? team.getCurrentMemberCount() : 0;
        if (currentMembers >= maxMembers) {
            return "????";
        }
        if (!Objects.equals(TeamStatusEnum.ACTIVE.getCode(), team.getTeamStatus())) {
            return "???????";
        }
        return null;
    }

    private void assertTeamNameUnique(String teamName) {
        Long count = teamDao.selectCount(new LambdaQueryWrapper<BasketTeam>()
                .eq(BasketTeam::getTeamName, teamName)
                .eq(BasketTeam::getIsDelete, NOT_DELETED));
        if (count != null && count > 0) {
            throw new ServiceException("???????");
        }
    }

    private BasketTeam requireReadableTeam(Long teamId, Long memberId) {
        BasketTeam team = teamDao.selectById(teamId);
        if (team == null || !NOT_DELETED.equals(team.getIsDelete())) {
            throw new ServiceException("?????");
        }
        BasketTeamMember membership = loadCurrentMembership(memberId, teamId);
        if (membership != null) {
            return team;
        }
        if (Objects.equals(TeamPublicStatusEnum.PUBLIC.getCode(), team.getPublicStatus())
                && Objects.equals(TeamStatusEnum.ACTIVE.getCode(), team.getTeamStatus())) {
            return team;
        }
        throw new ServiceException("???????", 403);
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
        return teamDao.selectBatchIds(teamIds).stream()
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
        for (BasketFileAsset asset : fileAssetDao.selectBatchIds(logoIds)) {
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
        } else {
            resp.setIsCaptain(false);
        }
        return resp;
    }

    private TeamDetailResp toDetailResp(BasketTeam team, BasketTeamMember membership, String logoUrl, boolean isCaptain) {
        TeamDetailResp resp = new TeamDetailResp();
        resp.setTeamId(toStringId(team.getId()));
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
        return resp;
    }

    private void validateCreateReq(TeamCreateReq req) {
        if (req == null || !StringUtils.hasText(req.getTeamName())) {
            throw new ServiceException("????????");
        }
        if (req.getTeamName().trim().length() > 50) {
            throw new ServiceException("??????");
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
