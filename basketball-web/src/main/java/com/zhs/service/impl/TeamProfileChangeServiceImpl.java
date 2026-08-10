package com.zhs.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketFileAssetDao;
import com.zhs.dao.BasketMemberProfileDao;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.BasketTeamDao;
import com.zhs.dao.BasketTeamMemberDao;
import com.zhs.dao.BasketTeamProfileChangeDao;
import com.zhs.enums.FileUsageTypeEnum;
import com.zhs.enums.TeamAuditStatusEnum;
import com.zhs.enums.TeamMemberRoleEnum;
import com.zhs.enums.TeamProfileChangeAuditStatusEnum;
import com.zhs.enums.TeamStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketFileAsset;
import com.zhs.model.BasketMemberProfile;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamMember;
import com.zhs.model.BasketTeamProfileChange;
import com.zhs.request.team.TeamProfileChangeCancelReq;
import com.zhs.request.team.TeamProfileChangeSubmitReq;
import com.zhs.response.team.TeamDetailResp;
import com.zhs.response.team.TeamProfileChangeDetailResp;
import com.zhs.response.team.TeamProfileChangeItemResp;
import com.zhs.response.team.TeamProfileChangeSubmitResp;
import com.zhs.response.team.TeamProfileEditResp;
import com.zhs.service.IAccountTeamService;
import com.zhs.service.ICaptainPermissionService;
import com.zhs.service.IAccountFileService;
import com.zhs.service.ITeamProfileChangeService;
import com.zhs.support.TeamWritableSupport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class TeamProfileChangeServiceImpl implements ITeamProfileChangeService {

    private static final Integer NOT_DELETED = 0;
    private static final int ACTIVE = 1;
    private static final int INACTIVE = 0;
    private static final List<Integer> PENDING_STATUSES = Arrays.asList(
            TeamProfileChangeAuditStatusEnum.PENDING.getCode(),
            TeamProfileChangeAuditStatusEnum.REVIEWING.getCode());

    @Resource
    private BasketTeamDao teamDao;
    @Resource
    private BasketTeamProfileChangeDao profileChangeDao;
    @Resource
    private BasketFileAssetDao fileAssetDao;
    @Resource
    private BasketMemberProfileDao memberProfileDao;
    @Resource
    private BasketPlayerProfileDao playerProfileDao;
    @Resource
    private BasketTeamMemberDao teamMemberDao;
    @Resource
    private ICaptainPermissionService captainPermissionService;
    @Resource
    private IAccountFileService accountFileService;
    @Resource
    private IAccountTeamService accountTeamService;
    @Resource
    private TeamWritableSupport teamWritableSupport;

    @Override
    public TeamProfileEditResp getProfileEditData(String teamId) {
        Long teamPk = parseLongId(teamId, "球队主键");
        Long memberId = UserContext.getRequiredUserId();
        captainPermissionService.requireCertifiedPlayer(memberId);
        BasketTeam team = requireReadableTeam(teamPk, memberId);
        boolean isCaptain = isCurrentCaptain(memberId, teamPk);

        TeamProfileEditResp resp = new TeamProfileEditResp();
        resp.setOfficialProfile(accountTeamService.getDetail(teamId));
        resp.setIsCaptain(isCaptain);
        resp.setCanSubmitChange(isCaptain && isTeamEligibleForChange(team));
        BasketTeamProfileChange pending = profileChangeDao.selectActiveByTeamId(teamPk);
        resp.setHasPendingChange(pending != null && TeamProfileChangeAuditStatusEnum.isPending(pending.getAuditStatus()));
        if (pending != null) {
            resp.setPendingChange(toDetailResp(pending, team));
        }
        resp.setRecentHistory(pageHistory(teamId, 1, 5, null, null));
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamProfileChangeSubmitResp submitChange(String teamId, TeamProfileChangeSubmitReq req) {
        if (req == null) {
            throw new ServiceException("请求参数不能为空");
        }
        Long teamPk = parseLongId(teamId, "球队主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketPlayerProfile player = captainPermissionService.requireCertifiedPlayer(memberId);
        BasketTeam team = captainPermissionService.requireTeamCaptain(memberId, teamPk);
        assertTeamEligibleForChange(team);

        if (!Objects.equals(TeamAuditStatusEnum.APPROVED.getCode(), team.getAuditStatus())) {
            throw new ServiceException("球队尚未认证通过，请直接在资料页保存");
        }

        String newTeamName = normalizeRequiredName(req.getTeamName());
        String newShortName = trimToNull(req.getTeamShortName());
        String newDescription = trimToNull(req.getDescription());
        Long newLogoFileId = resolveLogoFileId(req.getLogoFileId(), memberId, team.getLogoFileId());
        validateSubmitFields(newTeamName, newShortName, newDescription);

        if (isSameAsOfficial(team, newTeamName, newShortName, newDescription, newLogoFileId)) {
            throw new ServiceException("资料未发生变化");
        }

        assertTeamNameUnique(team, newTeamName, teamPk);

        Date now = new Date();
        BasketTeamProfileChange pending = profileChangeDao.selectActiveByTeamId(teamPk);
        if (pending != null && TeamProfileChangeAuditStatusEnum.isPending(pending.getAuditStatus())) {
            if (isSameAsPending(pending, newTeamName, newShortName, newDescription, newLogoFileId)) {
                throw new ServiceException("资料未发生变化");
            }
            pending.setNewTeamName(newTeamName);
            pending.setNewTeamShortName(newShortName);
            pending.setNewDescription(newDescription);
            pending.setNewLogoFileId(newLogoFileId);
            pending.setNewLogoUrl(resolveLogoUrl(newLogoFileId));
            pending.setChangeReason(trimToNull(req.getChangeReason()));
            pending.setUpdateTime(now);
            profileChangeDao.updateById(pending);
            return toSubmitResp(pending);
        }

        BasketTeamProfileChange change = new BasketTeamProfileChange();
        change.setChangeNo("TPC" + IdUtil.getSnowflakeNextIdStr());
        change.setTeamId(teamPk);
        change.setSubmitUserId(memberId);
        change.setSubmitPlayerId(player.getId());
        change.setOldTeamName(team.getTeamName());
        change.setNewTeamName(newTeamName);
        change.setOldTeamShortName(team.getShortName());
        change.setNewTeamShortName(newShortName);
        change.setOldLogoFileId(team.getLogoFileId());
        change.setNewLogoFileId(newLogoFileId);
        change.setOldLogoUrl(resolveLogoUrl(team.getLogoFileId()));
        change.setNewLogoUrl(resolveLogoUrl(newLogoFileId));
        change.setOldDescription(team.getDescription());
        change.setNewDescription(newDescription);
        change.setOldPublicStatus(team.getPublicStatus());
        change.setNewPublicStatus(team.getPublicStatus());
        change.setChangeReason(trimToNull(req.getChangeReason()));
        change.setAuditStatus(TeamProfileChangeAuditStatusEnum.PENDING.getCode());
        change.setActiveFlag(ACTIVE);
        change.setCreateTime(now);
        change.setUpdateTime(now);
        change.setIsDelete(NOT_DELETED);
        profileChangeDao.insert(change);
        return toSubmitResp(change);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelChange(String teamId, String changeId, TeamProfileChangeCancelReq req) {
        Long teamPk = parseLongId(teamId, "球队主键");
        Long changePk = parseLongId(changeId, "变更主键");
        Long memberId = UserContext.getRequiredUserId();
        captainPermissionService.requireTeamCaptain(memberId, teamPk);

        BasketTeamProfileChange change = requireTeamChange(teamPk, changePk);
        if (!Objects.equals(memberId, change.getSubmitUserId())
                && !isCurrentCaptain(memberId, teamPk)) {
            throw new ServiceException("无权取消该申请", 403);
        }
        if (!TeamProfileChangeAuditStatusEnum.isPending(change.getAuditStatus())) {
            throw new ServiceException("当前申请不可取消");
        }

        int rows = profileChangeDao.cancelIfMatch(changePk, teamPk, PENDING_STATUSES,
                TeamProfileChangeAuditStatusEnum.CANCELLED.getCode(),
                req != null ? trimToNull(req.getCancelReason()) : null, new Date());
        if (rows != 1) {
            throw new ServiceException("取消失败，申请状态已变更");
        }
    }

    @Override
    public List<TeamProfileChangeItemResp> pageHistory(String teamId, Integer pageNum, Integer pageSize,
                                                       Integer auditStatus, String keyword) {
        Long teamPk = parseLongId(teamId, "球队主键");
        Long memberId = UserContext.getRequiredUserId();
        captainPermissionService.requireTeamCaptain(memberId, teamPk);

        long page = normalizePage(pageNum);
        long size = normalizeSize(pageSize);
        long offset = (page - 1) * size;

        LambdaQueryWrapper<BasketTeamProfileChange> wrapper = buildHistoryWrapper(teamPk, auditStatus, keyword);
        wrapper.orderByDesc(BasketTeamProfileChange::getCreateTime)
                .orderByDesc(BasketTeamProfileChange::getId)
                .last("LIMIT " + size + " OFFSET " + offset);

        return profileChangeDao.selectList(wrapper).stream()
                .map(this::toItemResp)
                .collect(Collectors.toList());
    }

    @Override
    public long countHistory(String teamId, Integer auditStatus, String keyword) {
        Long teamPk = parseLongId(teamId, "球队主键");
        Long memberId = UserContext.getRequiredUserId();
        captainPermissionService.requireTeamCaptain(memberId, teamPk);
        Long count = profileChangeDao.selectCount(buildHistoryWrapper(teamPk, auditStatus, keyword));
        return count == null ? 0L : count;
    }

    @Override
    public TeamProfileChangeDetailResp getChangeDetail(String teamId, String changeId) {
        Long teamPk = parseLongId(teamId, "球队主键");
        Long changePk = parseLongId(changeId, "变更主键");
        Long memberId = UserContext.getRequiredUserId();
        captainPermissionService.requireTeamCaptain(memberId, teamPk);
        BasketTeam team = teamDao.selectById(teamPk);
        if (team == null || !NOT_DELETED.equals(team.getIsDelete())) {
            throw new ServiceException("球队不存在");
        }
        BasketTeamProfileChange change = requireTeamChange(teamPk, changePk);
        return toDetailResp(change, team);
    }

    private LambdaQueryWrapper<BasketTeamProfileChange> buildHistoryWrapper(Long teamPk,
                                                                            Integer auditStatus,
                                                                            String keyword) {
        LambdaQueryWrapper<BasketTeamProfileChange> wrapper = new LambdaQueryWrapper<BasketTeamProfileChange>()
                .eq(BasketTeamProfileChange::getTeamId, teamPk)
                .eq(BasketTeamProfileChange::getIsDelete, NOT_DELETED);
        if (auditStatus != null) {
            wrapper.eq(BasketTeamProfileChange::getAuditStatus, auditStatus);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(BasketTeamProfileChange::getChangeNo, kw)
                    .or().like(BasketTeamProfileChange::getNewTeamName, kw)
                    .or().like(BasketTeamProfileChange::getOldTeamName, kw)
                    .or().like(BasketTeamProfileChange::getAuditRemark, kw));
        }
        return wrapper;
    }

    private BasketTeamProfileChange requireTeamChange(Long teamPk, Long changePk) {
        BasketTeamProfileChange change = profileChangeDao.selectById(changePk);
        if (change == null || !NOT_DELETED.equals(change.getIsDelete())
                || !Objects.equals(teamPk, change.getTeamId())) {
            throw new ServiceException("变更申请不存在");
        }
        return change;
    }

    private BasketTeam requireReadableTeam(Long teamId, Long memberId) {
        BasketTeam team = teamDao.selectById(teamId);
        if (team == null || !NOT_DELETED.equals(team.getIsDelete())) {
            throw new ServiceException("球队不存在");
        }
        BasketTeamMember membership = teamMemberDao.selectOne(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getMemberId, memberId)
                .eq(BasketTeamMember::getTeamId, teamId)
                .eq(BasketTeamMember::getIsCurrent, 1)
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        if (membership != null) {
            return team;
        }
        if (Objects.equals(team.getCaptainMemberId(), memberId)) {
            return team;
        }
        throw new ServiceException("无权查看该球队资料变更", 403);
    }

    private boolean isCurrentCaptain(Long memberId, Long teamId) {
        BasketTeamMember membership = teamMemberDao.selectOne(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getMemberId, memberId)
                .eq(BasketTeamMember::getTeamId, teamId)
                .eq(BasketTeamMember::getIsCurrent, 1)
                .eq(BasketTeamMember::getMembershipStatus, 1)
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        return membership != null
                && TeamMemberRoleEnum.CAPTAIN.getCode().equals(membership.getRoleType());
    }

    private void assertTeamEligibleForChange(BasketTeam team) {
        if (!isTeamEligibleForChange(team)) {
            throw new ServiceException("当前球队状态不允许提交资料变更");
        }
    }

    private boolean isTeamEligibleForChange(BasketTeam team) {
        if (team == null
                || !NOT_DELETED.equals(team.getIsDelete())
                || !Objects.equals(TeamStatusEnum.ACTIVE.getCode(), team.getTeamStatus())
                || !Objects.equals(TeamAuditStatusEnum.APPROVED.getCode(), team.getAuditStatus())
                || team.getDivisionId() == null) {
            return false;
        }
        try {
            teamWritableSupport.assertTeamWritable(team);
            return true;
        } catch (ServiceException ex) {
            return false;
        }
    }

    private void validateSubmitFields(String teamName, String shortName, String description) {
        if (teamName.length() < 2 || teamName.length() > 30) {
            throw new ServiceException("球队名称长度应为 2-30 字");
        }
        if (shortName != null && (shortName.length() < 1 || shortName.length() > 12)) {
            throw new ServiceException("球队简称长度应为 1-12 字");
        }
        if (description != null && description.length() > 500) {
            throw new ServiceException("球队介绍不能超过 500 字");
        }
    }

    private String normalizeRequiredName(String teamName) {
        if (!StringUtils.hasText(teamName)) {
            throw new ServiceException("球队名称不能为空");
        }
        return teamName.trim();
    }

    private Long resolveLogoFileId(String logoFileId, Long memberId, Long fallbackLogoFileId) {
        if (!StringUtils.hasText(logoFileId)) {
            return fallbackLogoFileId;
        }
        Long fileId = parseLongId(logoFileId, "Logo 文件");
        BasketFileAsset asset = accountFileService.requireOwnedFile(fileId, memberId);
        if (!FileUsageTypeEnum.TEAM_LOGO.getCode().equalsIgnoreCase(asset.getUsageType())) {
            throw new ServiceException("文件用途不正确");
        }
        if (!StringUtils.hasText(asset.getMimeType())
                || !asset.getMimeType().toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new ServiceException("Logo 必须是图片文件");
        }
        return fileId;
    }

    private void assertTeamNameUnique(BasketTeam team, String teamName, Long excludeTeamId) {
        if (team == null || team.getDivisionId() == null) {
            throw new ServiceException("历史球队不能修改资料报名新赛事，请在当前赛事重新创建球队", 403);
        }
        Long count = teamDao.selectCount(new LambdaQueryWrapper<BasketTeam>()
                .eq(BasketTeam::getDivisionId, team.getDivisionId())
                .eq(BasketTeam::getTeamName, teamName)
                .eq(BasketTeam::getIsDelete, NOT_DELETED)
                .ne(BasketTeam::getId, excludeTeamId));
        if (count != null && count > 0) {
            throw new ServiceException("该赛事下球队名称已存在");
        }
    }

    private boolean isSameAsOfficial(BasketTeam team, String teamName, String shortName,
                                     String description, Long logoFileId) {
        return Objects.equals(team.getTeamName(), teamName)
                && Objects.equals(trimToNull(team.getShortName()), shortName)
                && Objects.equals(trimToNull(team.getDescription()), description)
                && Objects.equals(team.getLogoFileId(), logoFileId);
    }

    private boolean isSameAsPending(BasketTeamProfileChange pending, String teamName, String shortName,
                                    String description, Long logoFileId) {
        return Objects.equals(pending.getNewTeamName(), teamName)
                && Objects.equals(trimToNull(pending.getNewTeamShortName()), shortName)
                && Objects.equals(trimToNull(pending.getNewDescription()), description)
                && Objects.equals(pending.getNewLogoFileId(), logoFileId);
    }

    private TeamProfileChangeSubmitResp toSubmitResp(BasketTeamProfileChange change) {
        TeamProfileChangeSubmitResp resp = new TeamProfileChangeSubmitResp();
        resp.setChangeId(toStringId(change.getId()));
        resp.setChangeNo(change.getChangeNo());
        resp.setAuditStatus(change.getAuditStatus());
        TeamProfileChangeAuditStatusEnum status = TeamProfileChangeAuditStatusEnum.fromCode(change.getAuditStatus());
        resp.setAuditStatusText(status == null ? null : status.getLabel());
        return resp;
    }

    private TeamProfileChangeItemResp toItemResp(BasketTeamProfileChange change) {
        TeamProfileChangeItemResp resp = new TeamProfileChangeItemResp();
        resp.setChangeId(toStringId(change.getId()));
        resp.setChangeNo(change.getChangeNo());
        resp.setChangeSummary(buildChangeSummary(change));
        resp.setAuditStatus(change.getAuditStatus());
        TeamProfileChangeAuditStatusEnum status = TeamProfileChangeAuditStatusEnum.fromCode(change.getAuditStatus());
        resp.setAuditStatusText(status == null ? null : status.getLabel());
        resp.setSubmitTime(change.getCreateTime());
        resp.setAuditTime(change.getAuditTime());
        resp.setAuditRemark(change.getAuditRemark());
        return resp;
    }

    private TeamProfileChangeDetailResp toDetailResp(BasketTeamProfileChange change, BasketTeam team) {
        TeamProfileChangeDetailResp resp = new TeamProfileChangeDetailResp();
        resp.setChangeId(toStringId(change.getId()));
        resp.setChangeNo(change.getChangeNo());
        resp.setTeamId(toStringId(change.getTeamId()));
        resp.setOldTeamName(change.getOldTeamName());
        resp.setNewTeamName(change.getNewTeamName());
        resp.setOldTeamShortName(change.getOldTeamShortName());
        resp.setNewTeamShortName(change.getNewTeamShortName());
        resp.setOldLogoUrl(change.getOldLogoUrl());
        resp.setNewLogoUrl(change.getNewLogoUrl());
        resp.setOldDescription(change.getOldDescription());
        resp.setNewDescription(change.getNewDescription());
        resp.setOldPublicStatus(change.getOldPublicStatus());
        resp.setNewPublicStatus(change.getNewPublicStatus());
        resp.setChangeReason(change.getChangeReason());
        resp.setAuditStatus(change.getAuditStatus());
        TeamProfileChangeAuditStatusEnum status = TeamProfileChangeAuditStatusEnum.fromCode(change.getAuditStatus());
        resp.setAuditStatusText(status == null ? null : status.getLabel());
        resp.setAuditRemark(change.getAuditRemark());
        resp.setSubmitTime(change.getCreateTime());
        resp.setAuditTime(change.getAuditTime());
        resp.setChangeSummary(buildChangeSummary(change));
        resp.setSubmitUserId(toStringId(change.getSubmitUserId()));
        BasketMemberProfile memberProfile = memberProfileDao.selectOne(new LambdaQueryWrapper<BasketMemberProfile>()
                .eq(BasketMemberProfile::getMemberId, change.getSubmitUserId())
                .eq(BasketMemberProfile::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        if (memberProfile != null) {
            resp.setSubmitUserName(memberProfile.getNickname());
        }
        if (change.getSubmitPlayerId() != null) {
            BasketPlayerProfile player = playerProfileDao.selectById(change.getSubmitPlayerId());
            if (player != null) {
                resp.setSubmitPlayerNo(player.getPlayerNo());
            }
        }
        if (resp.getOldLogoUrl() == null && change.getOldLogoFileId() != null) {
            resp.setOldLogoUrl(resolveLogoUrl(change.getOldLogoFileId()));
        }
        if (resp.getNewLogoUrl() == null && change.getNewLogoFileId() != null) {
            resp.setNewLogoUrl(resolveLogoUrl(change.getNewLogoFileId()));
        }
        return resp;
    }

    private String buildChangeSummary(BasketTeamProfileChange change) {
        List<String> parts = new ArrayList<>();
        if (!Objects.equals(change.getOldTeamName(), change.getNewTeamName())) {
            parts.add("球队名称");
        }
        if (!Objects.equals(trimToNull(change.getOldTeamShortName()), trimToNull(change.getNewTeamShortName()))) {
            parts.add("球队简称");
        }
        if (!Objects.equals(change.getOldLogoFileId(), change.getNewLogoFileId())) {
            parts.add("Logo");
        }
        if (!Objects.equals(trimToNull(change.getOldDescription()), trimToNull(change.getNewDescription()))) {
            parts.add("球队介绍");
        }
        if (parts.isEmpty()) {
            return "资料变更";
        }
        return String.join("、", parts);
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

    private long normalizePage(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1L : pageNum;
    }

    private long normalizeSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 5L;
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
