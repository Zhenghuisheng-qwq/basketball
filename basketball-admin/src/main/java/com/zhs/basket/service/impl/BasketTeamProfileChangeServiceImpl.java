package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketTeam;
import com.zhs.basket.domain.BasketTeamProfileChange;
import com.zhs.basket.enums.TeamAuditStatusEnum;
import com.zhs.basket.enums.TeamProfileChangeAuditStatusEnum;
import com.zhs.basket.mapper.BasketTeamMapper;
import com.zhs.basket.mapper.BasketTeamProfileChangeMapper;
import com.zhs.basket.query.TeamProfileChangeQuery;
import com.zhs.basket.req.TeamProfileChangeApproveReq;
import com.zhs.basket.req.TeamProfileChangeRejectReq;
import com.zhs.basket.service.IBasketTeamProfileChangeService;
import com.zhs.basket.support.BasketAdminMessageService;
import com.zhs.basket.support.NoticeParams;
import com.zhs.basket.vo.TeamProfileChangeDetailVo;
import com.zhs.basket.vo.TeamProfileChangeListVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class BasketTeamProfileChangeServiceImpl implements IBasketTeamProfileChangeService {

    private static final Logger log = LoggerFactory.getLogger(BasketTeamProfileChangeServiceImpl.class);

    private static final List<Integer> PENDING_STATUSES = Arrays.asList(
            TeamProfileChangeAuditStatusEnum.PENDING.getCode(),
            TeamProfileChangeAuditStatusEnum.REVIEWING.getCode());

    @Autowired
    private BasketTeamProfileChangeMapper profileChangeMapper;
    @Autowired
    private BasketTeamMapper teamMapper;
    @Autowired
    private BasketAdminMessageService messageService;

    @Override
    public List<TeamProfileChangeListVo> selectList(TeamProfileChangeQuery query) {
        List<TeamProfileChangeListVo> list = profileChangeMapper.selectList(query);
        list.forEach(this::enrichListVo);
        return list;
    }

    @Override
    public TeamProfileChangeDetailVo selectDetail(Long id) {
        TeamProfileChangeDetailVo vo = profileChangeMapper.selectDetailVo(id);
        if (vo == null) {
            throw new ServiceException("球队资料变更申请不存在");
        }
        enrichDetailVo(vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, TeamProfileChangeApproveReq req) {
        BasketTeamProfileChange change = requirePendingChange(id);
        BasketTeam team = requireTeam(change.getTeamId());
        if (!Objects.equals(TeamAuditStatusEnum.APPROVED.getCode(), team.getAuditStatus())) {
            throw new ServiceException("仅已认证通过的球队可审核资料变更");
        }
        if (!StringUtils.hasText(change.getNewTeamShortName())) {
            throw new ServiceException("新球队简称不能为空");
        }
        String shortName = change.getNewTeamShortName().trim();
        assertTeamNameAvailable(team, change.getNewTeamName());
        assertLogoExists(change.getNewLogoFileId());
        Integer publicStatus = change.getNewPublicStatus() != null
                ? change.getNewPublicStatus() : team.getPublicStatus();
        if (publicStatus == null) {
            throw new ServiceException("球队公开状态不能为空");
        }

        int profileRows;
        try {
            profileRows = teamMapper.updateProfileFromChange(
                    team.getId(),
                    change.getNewTeamName(),
                    shortName,
                    change.getNewLogoFileId(),
                    change.getNewDescription(),
                    publicStatus);
        } catch (RuntimeException ex) {
            log.error("审核通过时更新正式球队资料失败, changeId={}, teamId={}", id, team.getId(), ex);
            throw new ServiceException("更新正式球队资料失败");
        }
        if (profileRows != 1) {
            throw new ServiceException("更新正式球队资料失败");
        }

        Date now = new Date();
        Long reviewerId = SecurityUtils.getUserId();
        int rows = profileChangeMapper.updateStatusIfMatch(id, PENDING_STATUSES,
                TeamProfileChangeAuditStatusEnum.APPROVED.getCode(), reviewerId,
                req != null ? req.getAuditRemark() : null, now, 0);
        assertSingleRow(rows, "审核通过失败，状态已变更");

        String teamName = StringUtils.hasText(change.getNewTeamName()) ? change.getNewTeamName() : team.getTeamName();
        messageService.sendSiteMessage(change.getSubmitUserId(), reviewerId, "TEAM_PROFILE_CHANGE", id,
                "球队资料变更已通过",
                "您的球队「" + teamName + "」资料变更已通过审核，正式资料已更新。",
                "TEAM_PROFILE_CHANGE_APPROVED", NoticeParams.json("teamName", teamName));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, TeamProfileChangeRejectReq req) {
        if (req == null || !StringUtils.hasText(req.getAuditRemark())) {
            throw new ServiceException("驳回原因不能为空");
        }
        BasketTeamProfileChange change = requirePendingChange(id);
        BasketTeam team = requireTeam(change.getTeamId());
        Date now = new Date();
        Long reviewerId = SecurityUtils.getUserId();
        int rows = profileChangeMapper.updateStatusIfMatch(id, PENDING_STATUSES,
                TeamProfileChangeAuditStatusEnum.REJECTED.getCode(), reviewerId,
                req.getAuditRemark().trim(), now, 0);
        assertSingleRow(rows, "驳回失败，状态已变更");

        String teamName = team.getTeamName();
        messageService.sendSiteMessage(change.getSubmitUserId(), reviewerId, "TEAM_PROFILE_CHANGE", id,
                "球队资料变更未通过",
                "您的球队「" + teamName + "」资料变更未通过审核，原因：" + req.getAuditRemark().trim(),
                "TEAM_PROFILE_CHANGE_REJECTED",
                NoticeParams.json("teamName", teamName, "remark", req.getAuditRemark().trim()));
    }

    private BasketTeamProfileChange requirePendingChange(Long id) {
        BasketTeamProfileChange change = profileChangeMapper.selectById(id);
        if (change == null) {
            throw new ServiceException("球队资料变更申请不存在");
        }
        if (!TeamProfileChangeAuditStatusEnum.isPending(change.getAuditStatus())) {
            throw new ServiceException("申请状态不可审核");
        }
        if (!Objects.equals(change.getActiveFlag(), 1)) {
            throw new ServiceException("申请已失效");
        }
        return change;
    }

    private BasketTeam requireTeam(Long teamId) {
        BasketTeam team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new ServiceException("球队不存在");
        }
        return team;
    }

    private void assertTeamNameAvailable(BasketTeam team, String teamName) {
        if (!StringUtils.hasText(teamName)) {
            throw new ServiceException("新球队名称不能为空");
        }
        if (team == null || team.getDivisionId() == null) {
            throw new ServiceException("历史球队不能修改资料");
        }
        if (teamMapper.countByTeamNameExclude(team.getDivisionId(), teamName.trim(), team.getId()) > 0) {
            throw new ServiceException("该赛事下球队名称已存在");
        }
    }

    private void assertLogoExists(Long logoFileId) {
        if (logoFileId == null) {
            return;
        }
    }

    private void enrichListVo(TeamProfileChangeListVo vo) {
        TeamProfileChangeAuditStatusEnum status = TeamProfileChangeAuditStatusEnum.fromCode(vo.getAuditStatus());
        vo.setAuditStatusName(status == null ? null : status.getLabel());
        vo.setChangeSummary(buildSummary(vo));
    }

    private void enrichDetailVo(TeamProfileChangeDetailVo vo) {
        TeamProfileChangeAuditStatusEnum status = TeamProfileChangeAuditStatusEnum.fromCode(vo.getAuditStatus());
        vo.setAuditStatusName(status == null ? null : status.getLabel());
        vo.setChangeSummary(buildSummary(vo));
    }

    private String buildSummary(TeamProfileChangeListVo vo) {
        List<String> parts = new ArrayList<>();
        if (!Objects.equals(vo.getOldTeamName(), vo.getNewTeamName())) {
            parts.add("球队名称");
        }
        if (!Objects.equals(trimToNull(vo.getOldTeamShortName()), trimToNull(vo.getNewTeamShortName()))) {
            parts.add("球队简称");
        }
        if (!Objects.equals(vo.getOldLogoFileId(), vo.getNewLogoFileId())) {
            parts.add("Logo");
        }
        if (!Objects.equals(trimToNull(vo.getOldDescription()), trimToNull(vo.getNewDescription()))) {
            parts.add("球队介绍");
        }
        return parts.isEmpty() ? "资料变更" : String.join("、", parts);
    }

    private String buildSummary(TeamProfileChangeDetailVo vo) {
        List<String> parts = new ArrayList<>();
        if (!Objects.equals(vo.getOldTeamName(), vo.getNewTeamName())) {
            parts.add("球队名称");
        }
        if (!Objects.equals(trimToNull(vo.getOldTeamShortName()), trimToNull(vo.getNewTeamShortName()))) {
            parts.add("球队简称");
        }
        if (!Objects.equals(trimToNull(vo.getOldDescription()), trimToNull(vo.getNewDescription()))) {
            parts.add("球队介绍");
        }
        if (!Objects.equals(vo.getOldLogoUrl(), vo.getNewLogoUrl())) {
            parts.add("Logo");
        }
        return parts.isEmpty() ? "资料变更" : String.join("、", parts);
    }

    private void assertSingleRow(int rows, String message) {
        if (rows != 1) {
            throw new ServiceException(message);
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
