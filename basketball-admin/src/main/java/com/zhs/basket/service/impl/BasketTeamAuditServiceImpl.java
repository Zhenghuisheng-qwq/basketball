package com.zhs.basket.service.impl;

import com.zhs.basket.domain.*;
import com.zhs.basket.enums.PlayerCertStatusEnum;
import com.zhs.basket.enums.TeamAuditStatusEnum;
import com.zhs.basket.enums.TeamMemberRoleEnum;
import com.zhs.basket.enums.TeamPublicStatusEnum;
import com.zhs.basket.enums.TeamStatusEnum;
import com.zhs.basket.mapper.*;
import com.zhs.basket.query.TeamAuditQuery;
import com.zhs.basket.req.BatchAuditOperationReq;
import com.zhs.basket.req.TeamAuditApproveReq;
import com.zhs.basket.req.TeamAuditRejectReq;
import com.zhs.basket.support.BatchAuditSupport;
import com.zhs.basket.vo.BatchAuditResultVo;
import com.zhs.basket.service.IBasketTeamAuditService;
import com.zhs.basket.support.BasketAdminMessageService;
import com.zhs.basket.support.TeamNoGenerator;
import com.zhs.basket.vo.AccountPlayerDetailVo;
import com.zhs.basket.vo.TeamAuditDetailVo;
import com.zhs.basket.vo.TeamAuditListVo;
import com.zhs.basket.vo.TeamAuditMemberVo;
import com.zhs.basket.vo.TeamAuditRecordVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.PageListUtils;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import com.zhs.common.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

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
    @Autowired private BasketMemberAccountMapper memberAccountMapper;
    @Autowired private BasketFileAssetMapper fileAssetMapper;
    @Autowired private BasketAdminMessageService messageService;
    @Autowired private TeamNoGenerator teamNoGenerator;

    @Override
    public List<TeamAuditListVo> selectList(TeamAuditQuery query) {
        List<BasketTeam> teams = teamMapper.selectAuditList(query);
        Map<Long, String> logoUrlMap = loadLogoUrlMap(teams.stream()
                .map(BasketTeam::getLogoFileId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        return PageListUtils.map(teams, team -> toListVo(team, logoUrlMap));
    }

    @Override
    public TeamAuditDetailVo selectDetail(Long teamId) {
        BasketTeam team = requireTeam(teamId);
        TeamAuditDetailVo vo = new TeamAuditDetailVo();
        BeanUtils.copyBeanProp(vo, team);
        vo.setLogoUrl(resolveLogoUrl(team.getLogoFileId(), loadLogoUrlMap(
                team.getLogoFileId() == null ? Collections.emptySet() : Collections.singleton(team.getLogoFileId()))));
        vo.setAuditStatusName(TeamAuditStatusEnum.fromCode(team.getAuditStatus()).getAuditDisplayDescription());
        vo.setPublicStatusName(resolvePublicStatusName(team.getPublicStatus()));
        vo.setSubmitTime(team.getCreateTime());

        enrichCaptainInfo(vo, team);
        enrichAuditInfo(vo, teamId);
        enrichMembers(vo, teamId);
        enrichValidationFlags(vo, team);

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

    @Override
    public BatchAuditResultVo batchApprove(BatchAuditOperationReq req) {
        TeamAuditApproveReq approveReq = new TeamAuditApproveReq();
        approveReq.setAuditRemark(req.getAuditRemark());
        return BatchAuditSupport.run(req.getIds(), id -> approve(id, approveReq));
    }

    @Override
    public BatchAuditResultVo batchReject(BatchAuditOperationReq req) {
        TeamAuditRejectReq rejectReq = new TeamAuditRejectReq();
        rejectReq.setRejectReason(req.getRejectReason());
        rejectReq.setAuditRemark(req.getAuditRemark());
        return BatchAuditSupport.run(req.getIds(), id -> reject(id, rejectReq));
    }

    private TeamAuditListVo toListVo(BasketTeam team, Map<Long, String> logoUrlMap) {
        TeamAuditListVo vo = new TeamAuditListVo();
        BeanUtils.copyBeanProp(vo, team);
        vo.setLogoUrl(resolveLogoUrl(team.getLogoFileId(), logoUrlMap));
        vo.setAuditStatusName(TeamAuditStatusEnum.fromCode(team.getAuditStatus()).getAuditDisplayDescription());
        vo.setPublicStatusName(resolvePublicStatusName(team.getPublicStatus()));
        return vo;
    }

    private void enrichCaptainInfo(TeamAuditDetailVo vo, BasketTeam team) {
        if (team.getCaptainMemberId() == null) {
            return;
        }
        AccountPlayerDetailVo captain = memberAccountMapper.selectDetail(team.getCaptainMemberId());
        if (captain != null) {
            vo.setCaptainNickname(captain.getNickname());
            vo.setCaptainEmail(captain.getEmail());
            if (captain.getPlayerNo() != null) {
                vo.setCaptainPlayerNo(captain.getPlayerNo());
            }
            if (captain.getCertificationStatus() != null) {
                vo.setCaptainCertStatus(captain.getCertificationStatus());
                vo.setCaptainCertStatusName(PlayerCertStatusEnum.fromCode(captain.getCertificationStatus()).getDescription());
            }
            if (captain.getPlayerId() != null) {
                vo.setCaptainPlayerId(captain.getPlayerId());
            }
        } else if (team.getCaptainPlayerId() != null) {
            BasketPlayerProfile profile = profileMapper.selectById(team.getCaptainPlayerId());
            if (profile != null) {
                vo.setCaptainPlayerNo(profile.getPlayerNo());
                vo.setCaptainCertStatus(profile.getCertificationStatus());
                vo.setCaptainCertStatusName(PlayerCertStatusEnum.fromCode(profile.getCertificationStatus()).getDescription());
            }
        }
        Long playerId = vo.getCaptainPlayerId() != null ? vo.getCaptainPlayerId() : team.getCaptainPlayerId();
        if (playerId != null) {
            vo.setCaptainQualificationValid(qualificationMapper.countCurrentActiveByPlayerId(playerId) > 0);
        }
    }

    private void enrichAuditInfo(TeamAuditDetailVo vo, Long teamId) {
        List<BasketTeamAuditRecord> records = auditRecordMapper.selectByTeamId(teamId);
        if (CollectionUtils.isEmpty(records)) {
            return;
        }
        BasketTeamAuditRecord latestDecision = records.stream()
                .filter(r -> "APPROVE".equals(r.getAuditAction()) || "REJECT".equals(r.getAuditAction()))
                .findFirst()
                .orElse(null);
        if (latestDecision != null) {
            vo.setAuditTime(latestDecision.getAuditTime());
            vo.setAuditUserName(latestDecision.getAuditorName());
            vo.setAuditRemark(latestDecision.getAuditRemark());
            if ("REJECT".equals(latestDecision.getAuditAction())) {
                vo.setRejectReason(latestDecision.getAuditRemark());
            }
        }
    }

    private void enrichMembers(TeamAuditDetailVo vo, Long teamId) {
        List<TeamAuditMemberVo> members = teamMemberMapper.selectCurrentMembersForAudit(teamId);
        for (TeamAuditMemberVo member : members) {
            if (TeamMemberRoleEnum.CAPTAIN.getCode().equals(member.getRoleType())) {
                member.setRoleTypeName("队长");
            } else if (TeamMemberRoleEnum.MEMBER.getCode().equals(member.getRoleType())) {
                member.setRoleTypeName("成员");
            } else {
                member.setRoleTypeName(member.getRoleType());
            }
        }
        vo.setMembers(members);
    }

    private void enrichValidationFlags(TeamAuditDetailVo vo, BasketTeam team) {
        vo.setCaptainAssigned(team.getCaptainMemberId() != null && team.getCaptainPlayerId() != null);
        vo.setCaptainCertified(vo.getCaptainCertStatus() != null
                && Objects.equals(PlayerCertStatusEnum.APPROVED.getCode(), vo.getCaptainCertStatus()));
        vo.setTeamActive(Objects.equals(TeamStatusEnum.ACTIVE.getCode(), team.getTeamStatus())
                && Objects.equals(NOT_DELETED, team.getIsDelete()));
    }

    private String resolvePublicStatusName(Integer publicStatus) {
        if (publicStatus == null) {
            return "-";
        }
        try {
            return TeamPublicStatusEnum.fromCode(publicStatus).getDescription();
        } catch (ServiceException ex) {
            return "-";
        }
    }

    private Map<Long, String> loadLogoUrlMap(Set<Long> fileIds) {
        if (CollectionUtils.isEmpty(fileIds)) {
            return Collections.emptyMap();
        }
        Map<Long, String> map = new HashMap<>();
        for (Long fileId : fileIds) {
            BasketFileAsset asset = fileAssetMapper.selectById(fileId);
            if (asset != null && NOT_DELETED.equals(asset.getIsDelete()) && StringUtils.hasText(asset.getAccessUrl())) {
                map.put(fileId, asset.getAccessUrl());
            }
        }
        return map;
    }

    private String resolveLogoUrl(Long logoFileId, Map<Long, String> logoUrlMap) {
        return logoFileId == null ? null : logoUrlMap.get(logoFileId);
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
