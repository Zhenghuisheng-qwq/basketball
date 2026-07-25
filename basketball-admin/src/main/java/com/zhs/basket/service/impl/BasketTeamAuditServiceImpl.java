package com.zhs.basket.service.impl;

import com.zhs.basket.domain.*;
import com.zhs.basket.enums.PlayerCertStatusEnum;
import com.zhs.basket.enums.TeamAuditStatusEnum;
import com.zhs.basket.enums.TeamMemberRoleEnum;
import com.zhs.basket.mapper.*;
import com.zhs.basket.query.TeamAuditQuery;
import com.zhs.basket.req.TeamAuditApproveReq;
import com.zhs.basket.req.TeamAuditRejectReq;
import com.zhs.basket.service.IBasketTeamAuditService;
import com.zhs.basket.support.BasketAdminMessageService;
import com.zhs.basket.support.TeamNoGenerator;
import com.zhs.basket.vo.TeamAuditDetailVo;
import com.zhs.basket.vo.TeamAuditListVo;
import com.zhs.basket.vo.TeamAuditRecordVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import com.zhs.common.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BasketTeamAuditServiceImpl implements IBasketTeamAuditService {

    private static final Integer NOT_DELETED = 0;
    private static final String TEAM_NO_PREFIX = "CNPA-T-";

    @Autowired private BasketTeamMapper teamMapper;
    @Autowired private BasketTeamAuditRecordMapper auditRecordMapper;
    @Autowired private BasketTeamMemberMapper teamMemberMapper;
    @Autowired private BasketPlayerProfileMapper profileMapper;
    @Autowired private BasketCaptainQualificationMapper qualificationMapper;
    @Autowired private BasketAdminMessageService messageService;
    @Autowired private TeamNoGenerator teamNoGenerator;

    @Override
    public List<TeamAuditListVo> selectList(TeamAuditQuery query) {
        return teamMapper.selectAuditList(query).stream().map(t -> {
            TeamAuditListVo vo = new TeamAuditListVo();
            BeanUtils.copyBeanProp(vo, t);
            vo.setAuditStatusName(TeamAuditStatusEnum.fromCode(t.getAuditStatus()).getDescription());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public TeamAuditDetailVo selectDetail(Long teamId) {
        BasketTeam team = requireTeam(teamId);
        TeamAuditDetailVo vo = new TeamAuditDetailVo();
        BeanUtils.copyBeanProp(vo, team);
        vo.setAuditStatusName(TeamAuditStatusEnum.fromCode(team.getAuditStatus()).getDescription());
        return vo;
    }

    @Override
    public List<TeamAuditRecordVo> selectAuditRecords(Long teamId) {
        return auditRecordMapper.selectByTeamId(teamId).stream().map(r -> {
            TeamAuditRecordVo vo = new TeamAuditRecordVo();
            BeanUtils.copyBeanProp(vo, r);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startReview(Long teamId) {
        BasketTeam team = requireTeam(teamId);
        int rows = teamMapper.updateAuditStatusIfMatch(teamId,
                Collections.singletonList(TeamAuditStatusEnum.PENDING.getCode()),
                TeamAuditStatusEnum.REVIEWING.getCode(), SecurityUtils.getUserId());
        assertSingleRow(rows, "开始审核失败");
        writeAudit(team, "START_REVIEW", team.getAuditStatus(), TeamAuditStatusEnum.REVIEWING.getCode(), "开始审核");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long teamId, TeamAuditApproveReq req) {
        BasketTeam team = requireTeam(teamId);
        validateCaptainForApprove(team);
        boolean alreadyApproved = TeamAuditStatusEnum.APPROVED.getCode().equals(team.getAuditStatus());
        if (!alreadyApproved) {
            int rows = teamMapper.updateAuditStatusIfMatch(teamId,
                    Arrays.asList(TeamAuditStatusEnum.PENDING.getCode(), TeamAuditStatusEnum.REVIEWING.getCode()),
                    TeamAuditStatusEnum.APPROVED.getCode(), SecurityUtils.getUserId());
            assertSingleRow(rows, "审核通过失败");
            int memberCount = teamMemberMapper.countCurrentMembers(teamId);
            teamMapper.updateMemberCount(teamId, memberCount);
            writeAudit(team, "APPROVE", team.getAuditStatus(), TeamAuditStatusEnum.APPROVED.getCode(), req.getAuditRemark());
            messageService.sendSiteMessage(team.getCaptainMemberId(), SecurityUtils.getUserId(), "TEAM_AUDIT", teamId,
                    "球队审核已通过", "您的球队「" + team.getTeamName() + "」已通过审核。");
        }
        ensureTeamNo(team);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long teamId, TeamAuditRejectReq req) {
        BasketTeam team = requireTeam(teamId);
        String remark = StringUtils.hasText(req.getRejectReason()) ? req.getRejectReason() : req.getAuditRemark();
        int rows = teamMapper.updateAuditStatusIfMatch(teamId,
                Arrays.asList(TeamAuditStatusEnum.PENDING.getCode(), TeamAuditStatusEnum.REVIEWING.getCode()),
                TeamAuditStatusEnum.REJECTED.getCode(), SecurityUtils.getUserId());
        assertSingleRow(rows, "驳回失败");
        writeAudit(team, "REJECT", team.getAuditStatus(), TeamAuditStatusEnum.REJECTED.getCode(), remark);
        messageService.sendSiteMessage(team.getCaptainMemberId(), SecurityUtils.getUserId(), "TEAM_AUDIT", teamId,
                "球队审核已驳回", remark);
    }

    private void ensureTeamNo(BasketTeam team) {
        if (StringUtils.hasText(team.getTeamNo()) && team.getTeamNo().startsWith(TEAM_NO_PREFIX)) {
            return;
        }
        teamMapper.updateTeamNo(team.getId(), teamNoGenerator.generateTeamNo());
    }

    private void validateCaptainForApprove(BasketTeam team) {
        BasketTeamMember captain = teamMemberMapper.selectCaptainMember(team.getId());
        if (captain == null || !TeamMemberRoleEnum.CAPTAIN.getCode().equals(captain.getRoleType())) {
            throw new ServiceException("缺少有效队长成员");
        }
        BasketPlayerProfile profile = profileMapper.selectById(captain.getPlayerId());
        if (profile == null || !Objects.equals(profile.getCertificationStatus(), PlayerCertStatusEnum.APPROVED.getCode())) {
            throw new ServiceException("队长未完成球员认证");
        }
        if (qualificationMapper.countCurrentActiveByPlayerId(captain.getPlayerId()) <= 0) {
            throw new ServiceException("队长缺少有效队长资格");
        }
    }

    private void writeAudit(BasketTeam team, String action, Integer before, Integer after, String remark) {
        Date now = new Date();
        BasketTeamAuditRecord record = new BasketTeamAuditRecord();
        record.setTeamId(team.getId());
        record.setAuditType("TEAM_CREATE");
        record.setAuditAction(action);
        record.setBeforeStatus(before);
        record.setAfterStatus(after);
        record.setAuditorId(SecurityUtils.getUserId());
        record.setAuditorName(SecurityUtils.getUsername());
        record.setAuditRemark(remark);
        record.setAuditTime(now);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        record.setIsDelete(NOT_DELETED);
        auditRecordMapper.insert(record);
    }

    private BasketTeam requireTeam(Long id) {
        BasketTeam team = teamMapper.selectById(id);
        if (team == null) {
            throw new ServiceException("球队不存在");
        }
        return team;
    }

    private void assertSingleRow(int rows, String message) {
        if (rows != 1) {
            throw new ServiceException(message);
        }
    }
}
