package com.zhs.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.common.UserContext;
import com.zhs.config.AccountTeamProperties;
import com.zhs.dao.BasketFileAssetDao;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.BasketTeamDao;
import com.zhs.dao.BasketTeamMemberDao;
import com.zhs.enums.FileUsageTypeEnum;
import com.zhs.enums.TeamAuditStatusEnum;
import com.zhs.enums.TeamJoinSourceEnum;
import com.zhs.enums.TeamMemberRoleEnum;
import com.zhs.enums.TeamMembershipStatusEnum;
import com.zhs.enums.TeamPublicStatusEnum;
import com.zhs.enums.TeamRosterTypeEnum;
import com.zhs.enums.TeamStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketCaptainQualification;
import com.zhs.model.BasketFileAsset;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamMember;
import com.zhs.request.team.TeamCreateReq;
import com.zhs.request.team.TeamLogoBindReq;
import com.zhs.request.team.TeamUpdateReq;
import com.zhs.response.team.TeamDetailResp;
import com.zhs.response.team.TeamSummaryResp;
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
    private BasketPlayerProfileDao playerProfileDao;
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
    public IPage<TeamSummaryResp> pageJoinable(Integer currentPage, Integer pageSize, String keyword) {
        Long memberId = UserContext.getRequiredUserId();
        Set<Long> joinedTeamIds = loadJoinedTeamIds(memberId);

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
        List<BasketTeam> joinable = allTeams.stream()
                .filter(team -> !joinedTeamIds.contains(team.getId()))
                .filter(team -> team.getCurrentMemberCount() == null
                        || team.getMaxMemberCount() == null
                        || team.getCurrentMemberCount() < team.getMaxMemberCount())
                .collect(Collectors.toList());

        Map<Long, String> logoUrlMap = loadLogoUrlMap(joinable);
        List<TeamSummaryResp> items = joinable.stream()
                .map(team -> toSummaryResp(team, null, logoUrlMap.get(team.getLogoFileId())))
                .collect(Collectors.toList());
        return toPage(items, currentPage, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamDetailResp create(TeamCreateReq req, String clientIp) {
        validateCreateReq(req);
        Long memberId = UserContext.getRequiredUserId();
        BasketCaptainQualification qualification = captainPermissionService.requireValidQualification(memberId);
        BasketPlayerProfile playerProfile = captainPermissionService.requireCertifiedPlayer(memberId);

        assertNoCurrentMembership(memberId);
        assertNotCaptainOfOtherActiveTeam(memberId);
        assertTeamNameUnique(req.getTeamName().trim());

        Date now = new Date();
        BasketTeam team = new BasketTeam();
        team.setTeamNo("TM" + IdUtil.getSnowflakeNextIdStr());
        team.setTeamName(req.getTeamName().trim());
        team.setShortName(trimToNull(req.getShortName()));
        team.setGameTerminal(trimToNull(req.getGameTerminal()));
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
            Long logoFileId = parseLongId(req.getLogoFileId(), "Logo 文件主键");
            bindTeamLogoFile(team, logoFileId, memberId);
        }

        teamDao.insert(team);

        BasketTeamMember captainMember = new BasketTeamMember();
        captainMember.setMembershipNo("MB" + IdUtil.getSnowflakeNextIdStr());
        captainMember.setTeamId(team.getId());
        captainMember.setMemberId(memberId);
        captainMember.setPlayerId(playerProfile.getId());
        captainMember.setRoleType(TeamMemberRoleEnum.CAPTAIN.getCode());
        captainMember.setRosterType(TeamRosterTypeEnum.OFFICIAL.getCode());
        captainMember.setJoinSource(TeamJoinSourceEnum.CREATE.getCode());
        captainMember.setMembershipStatus(TeamMembershipStatusEnum.ACTIVE.getCode());
        captainMember.setIsCurrent(IS_CURRENT);
        captainMember.setJoinTime(now);
        captainMember.setIsDelete(NOT_DELETED);
        teamMemberDao.insert(captainMember);

        updatePlayerCurrentTeam(playerProfile, team.getId());
        securityLogHelper.write(memberId, EVENT_TEAM_CREATE, "创建球队",
                null, team.getTeamName(), memberId, clientIp);

        BasketTeamMember membership = captainMember;
        return toDetailResp(team, membership, resolveLogoUrl(team.getLogoFileId()), true);
    }

    @Override
    public TeamDetailResp getDetail(String teamId) {
        Long teamPk = parseLongId(teamId, "球队主键");
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
            throw new ServiceException("请求参数不能为空");
        }
        Long teamPk = parseLongId(teamId, "球队主键");
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
        securityLogHelper.write(memberId, EVENT_TEAM_UPDATE, "更新球队信息",
                oldName, team.getTeamName(), memberId, clientIp);

        BasketTeamMember membership = loadCurrentMembership(memberId, teamPk);
        return toDetailResp(team, membership, resolveLogoUrl(team.getLogoFileId()), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamDetailResp bindLogo(String teamId, TeamLogoBindReq req, String clientIp) {
        if (req == null || !StringUtils.hasText(req.getFileId())) {
            throw new ServiceException("文件主键不能为空");
        }
        Long teamPk = parseLongId(teamId, "球队主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTeam team = captainPermissionService.requireTeamCaptain(memberId, teamPk);
        if (Objects.equals(TeamAuditStatusEnum.APPROVED.getCode(), team.getAuditStatus())) {
            throw new ServiceException("球队已审核通过，不可修改 Logo");
        }

        Long fileId = parseLongId(req.getFileId(), "文件主键");
        bindTeamLogoFile(team, fileId, memberId);
        teamDao.updateById(team);
        securityLogHelper.write(memberId, EVENT_TEAM_LOGO, "更新球队 Logo",
                null, team.getTeamName(), memberId, clientIp);

        BasketTeamMember membership = loadCurrentMembership(memberId, teamPk);
        return toDetailResp(team, membership, resolveLogoUrl(team.getLogoFileId()), true);
    }

    private void bindTeamLogoFile(BasketTeam team, Long fileId, Long memberId) {
        BasketFileAsset asset = accountFileService.requireOwnedImageFile(fileId, memberId);
        if (!FileUsageTypeEnum.TEAM_LOGO.getCode().equalsIgnoreCase(asset.getUsageType())) {
            throw new ServiceException("文件用途不匹配");
        }
        team.setLogoFileId(fileId);
    }

    private void assertNoCurrentMembership(Long memberId) {
        Long count = teamMemberDao.selectCount(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getMemberId, memberId)
                .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                .eq(BasketTeamMember::getMembershipStatus, TeamMembershipStatusEnum.ACTIVE.getCode())
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED));
        if (count != null && count > 0) {
            throw new ServiceException("您已在其他球队中，无法创建新球队");
        }
    }

    private void assertNotCaptainOfOtherActiveTeam(Long memberId) {
        Long count = teamDao.selectCount(new LambdaQueryWrapper<BasketTeam>()
                .eq(BasketTeam::getCaptainMemberId, memberId)
                .eq(BasketTeam::getTeamStatus, TeamStatusEnum.ACTIVE.getCode())
                .eq(BasketTeam::getIsDelete, NOT_DELETED));
        if (count != null && count > 0) {
            throw new ServiceException("您已是其他球队的队长，无法创建新球队");
        }
    }

    private void assertTeamNameUnique(String teamName) {
        Long count = teamDao.selectCount(new LambdaQueryWrapper<BasketTeam>()
                .eq(BasketTeam::getTeamName, teamName)
                .eq(BasketTeam::getIsDelete, NOT_DELETED));
        if (count != null && count > 0) {
            throw new ServiceException("球队名称已存在");
        }
    }

    private BasketTeam requireReadableTeam(Long teamId, Long memberId) {
        BasketTeam team = teamDao.selectById(teamId);
        if (team == null || !NOT_DELETED.equals(team.getIsDelete())) {
            throw new ServiceException("球队不存在");
        }
        BasketTeamMember membership = loadCurrentMembership(memberId, teamId);
        if (membership != null) {
            return team;
        }
        if (Objects.equals(TeamPublicStatusEnum.PUBLIC.getCode(), team.getPublicStatus())
                && Objects.equals(TeamStatusEnum.ACTIVE.getCode(), team.getTeamStatus())) {
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

    private TeamSummaryResp toSummaryResp(BasketTeam team, BasketTeamMember membership, String logoUrl) {
        TeamSummaryResp resp = new TeamSummaryResp();
        resp.setTeamId(toStringId(team.getId()));
        resp.setTeamNo(team.getTeamNo());
        resp.setTeamName(team.getTeamName());
        resp.setShortName(team.getShortName());
        resp.setLogoUrl(logoUrl);
        resp.setRegionName(team.getRegionName());
        resp.setGameTerminal(team.getGameTerminal());
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
            throw new ServiceException("球队名称不能为空");
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
