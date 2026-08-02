package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketMatch;
import com.zhs.basket.domain.BasketMatchDataSubmission;
import com.zhs.basket.domain.BasketMatchDataSubmissionMaterial;
import com.zhs.basket.domain.BasketMatchDataSubmissionPeriod;
import com.zhs.basket.domain.BasketMatchDataSubmissionPlayerStat;
import com.zhs.basket.domain.BasketMatchDataSubmissionTeamStat;
import com.zhs.basket.enums.MatchDataAuditActionEnum;
import com.zhs.basket.enums.MatchDataSubmissionStatusEnum;
import com.zhs.basket.enums.MatchPeriodTypeEnum;
import com.zhs.basket.enums.MatchStatusEnum;
import com.zhs.basket.mapper.BasketMatchDataAuditMapper;
import com.zhs.basket.mapper.BasketMatchDataSubmissionMapper;
import com.zhs.basket.mapper.BasketMatchDataSubmissionMaterialMapper;
import com.zhs.basket.mapper.BasketMatchDataSubmissionPeriodMapper;
import com.zhs.basket.mapper.BasketMatchDataSubmissionPlayerStatMapper;
import com.zhs.basket.mapper.BasketMatchDataSubmissionTeamStatMapper;
import com.zhs.basket.mapper.BasketMatchMapper;
import com.zhs.basket.req.AdminMatchDataSubmissionApproveReq;
import com.zhs.basket.req.AdminMatchDataSubmissionPageReq;
import com.zhs.basket.req.AdminMatchDataSubmissionRejectReq;
import com.zhs.basket.service.IBasketMatchDataSubmissionApplyService;
import com.zhs.basket.service.IBasketMatchDataSubmissionAuditService;
import com.zhs.basket.service.IBasketMatchDataSubmissionValidationService;
import com.zhs.basket.service.IBasketMatchValidationService;
import com.zhs.basket.support.BasketMatchDataAuditLogSupport;
import com.zhs.basket.support.BasketMatchMessageHelper;
import com.zhs.basket.vo.MatchDataSubmissionAuditLogVo;
import com.zhs.basket.vo.MatchDataSubmissionDetailVo;
import com.zhs.basket.vo.MatchDataSubmissionListVo;
import com.zhs.basket.vo.MatchDataSubmissionMaterialVo;
import com.zhs.basket.vo.MatchDataSubmissionPeriodVo;
import com.zhs.basket.vo.MatchDataSubmissionPlayerStatVo;
import com.zhs.basket.vo.MatchDataSubmissionTeamStatVo;
import com.zhs.basket.vo.MatchDataSubmissionVersionVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class BasketMatchDataSubmissionAuditServiceImpl implements IBasketMatchDataSubmissionAuditService {

    private static final String AUDIT_PENDING = "PENDING";
    private static final String AUDIT_APPROVED = "APPROVED";
    private static final String AUDIT_REJECTED = "REJECTED";

    @Autowired private BasketMatchDataSubmissionMapper submissionMapper;
    @Autowired private BasketMatchDataSubmissionPeriodMapper periodMapper;
    @Autowired private BasketMatchDataSubmissionTeamStatMapper teamStatMapper;
    @Autowired private BasketMatchDataSubmissionPlayerStatMapper playerStatMapper;
    @Autowired private BasketMatchDataSubmissionMaterialMapper materialMapper;
    @Autowired private BasketMatchDataAuditMapper auditMapper;
    @Autowired private BasketMatchMapper matchMapper;
    @Autowired private IBasketMatchValidationService validationService;
    @Autowired private IBasketMatchDataSubmissionValidationService submissionValidationService;
    @Autowired private IBasketMatchDataSubmissionApplyService applyService;
    @Autowired private BasketMatchDataAuditLogSupport auditLogSupport;
    @Autowired private BasketMatchMessageHelper messageHelper;

    @Override
    public List<MatchDataSubmissionListVo> pageSubmissions(AdminMatchDataSubmissionPageReq query) {
        AdminMatchDataSubmissionPageReq effective = query != null ? query : new AdminMatchDataSubmissionPageReq();
        if (effective.getSubmissionStatus() == null && StringUtils.isEmpty(effective.getAuditStatus())) {
            effective.setSubmissionStatus(MatchDataSubmissionStatusEnum.SUBMITTED.getCode());
            effective.setAuditStatus(AUDIT_PENDING);
        }
        List<MatchDataSubmissionListVo> list = submissionMapper.selectListForAudit(effective);
        for (MatchDataSubmissionListVo vo : list) {
            vo.setSubmissionStatusName(MatchDataSubmissionStatusEnum.fromCode(vo.getSubmissionStatus()).getDescription());
            vo.setRiskFlag(Boolean.FALSE);
        }
        return list;
    }

    @Override
    public MatchDataSubmissionDetailVo getDetail(Long submissionId) {
        BasketMatchDataSubmission submission = requireSubmission(submissionId);
        BasketMatch match = validationService.requireMatch(submission.getMatchId());
        MatchDataSubmissionDetailVo vo = buildBaseDetail(submission, match);
        com.zhs.basket.vo.MatchDetailVo matchDetail = matchMapper.selectDetailById(submission.getMatchId());
        if (matchDetail != null) {
            vo.setSeasonName(matchDetail.getSeasonName());
            vo.setStageName(matchDetail.getStageName());
            vo.setGroupName(matchDetail.getGroupName());
            vo.setHomeTeamName(matchDetail.getHomeTeamName());
            vo.setAwayTeamName(matchDetail.getAwayTeamName());
        }
        vo.setPeriods(buildPeriodVos(periodMapper.selectBySubmissionId(submissionId)));
        vo.setTeamStats(teamStatMapper.selectVoBySubmissionId(submissionId));
        vo.setPlayerStats(playerStatMapper.selectVoBySubmissionId(submissionId));
        vo.setMaterials(materialMapper.selectVoBySubmissionId(submissionId));
        vo.setVersions(submissionMapper.selectVersionsByMatchId(submission.getMatchId()));
        vo.setAuditLogs(buildAuditLogs(submissionId));
        fillAuditFlags(vo, submission, match);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long submissionId, AdminMatchDataSubmissionApproveReq req) {
        Long operatorId = SecurityUtils.getUserId();
        String operatorName = SecurityUtils.getUsername();
        String auditRemark = req != null ? req.getAuditRemark() : null;

        BasketMatchDataSubmission submission = submissionMapper.selectByIdForUpdate(submissionId);
        if (submission == null) {
            throw new ServiceException("提交记录不存在");
        }
        assertPendingSubmission(submission);

        BasketMatch match = matchMapper.selectByIdForUpdate(submission.getMatchId());
        if (match == null) {
            throw new ServiceException("比赛不存在");
        }
        if (match.getMatchStatus() == null || match.getMatchStatus() != MatchStatusEnum.FINISHED.getCode()) {
            throw new ServiceException("比赛未结束，无法审核通过");
        }
        BasketMatchDataSubmission active = submissionMapper.selectActiveByMatchIdAndSubmitterTeamId(
                submission.getMatchId(), submission.getSubmitterTeamId());
        if (active != null && !active.getId().equals(submissionId)) {
            throw new ServiceException("当前提交方已有其他有效提交");
        }

        List<BasketMatchDataSubmissionPeriod> periods = periodMapper.selectBySubmissionId(submissionId);
        List<BasketMatchDataSubmissionTeamStat> teamStats = teamStatMapper.selectBySubmissionId(submissionId);
        List<BasketMatchDataSubmissionPlayerStat> playerStats = playerStatMapper.selectBySubmissionId(submissionId);
        List<BasketMatchDataSubmissionMaterial> materials = materialMapper.selectBySubmissionId(submissionId);
        submissionValidationService.validateSubmissionData(submission, periods, teamStats, playerStats, materials);

        String beforeStatus = String.valueOf(submission.getSubmissionStatus());
        if (submissionMapper.updateStatusIfMatch(submissionId,
                MatchDataSubmissionStatusEnum.SUBMITTED.getCode(), AUDIT_PENDING,
                MatchDataSubmissionStatusEnum.APPROVED.getCode(), AUDIT_APPROVED,
                operatorId, operatorName, null, auditRemark) != 1) {
            throw new ServiceException("审核通过失败，提交可能已被其他管理员处理");
        }

        writeAuditLog(submission, MatchDataAuditActionEnum.APPROVE, beforeStatus,
                String.valueOf(MatchDataSubmissionStatusEnum.APPROVED.getCode()),
                AUDIT_APPROVED, operatorId, operatorName, auditRemark, null);

        applyService.applyApprovedSubmissionToOfficialData(submissionId, operatorId);

        match = matchMapper.selectById(submission.getMatchId());
        messageHelper.notifyCaptains(match, operatorId, "比赛数据审核通过",
                "比赛 " + match.getMatchNo() + " 提交的数据已审核通过并生效。");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long submissionId, AdminMatchDataSubmissionRejectReq req) {
        if (req == null || StringUtils.isEmpty(req.getRejectReason())) {
            throw new ServiceException("驳回原因不能为空");
        }
        Long operatorId = SecurityUtils.getUserId();
        String operatorName = SecurityUtils.getUsername();

        BasketMatchDataSubmission submission = submissionMapper.selectByIdForUpdate(submissionId);
        if (submission == null) {
            throw new ServiceException("提交记录不存在");
        }
        assertPendingSubmission(submission);

        matchMapper.selectByIdForUpdate(submission.getMatchId());

        String beforeStatus = String.valueOf(submission.getSubmissionStatus());
        if (submissionMapper.updateStatusIfMatch(submissionId,
                MatchDataSubmissionStatusEnum.SUBMITTED.getCode(), AUDIT_PENDING,
                MatchDataSubmissionStatusEnum.REJECTED.getCode(), AUDIT_REJECTED,
                operatorId, operatorName, req.getRejectReason(), req.getAuditRemark()) != 1) {
            throw new ServiceException("审核驳回失败，提交可能已被其他管理员处理");
        }

        writeAuditLog(submission, MatchDataAuditActionEnum.REJECT, beforeStatus,
                String.valueOf(MatchDataSubmissionStatusEnum.REJECTED.getCode()),
                AUDIT_REJECTED, operatorId, operatorName, req.getAuditRemark(), req.getRejectReason());

        BasketMatch match = matchMapper.selectById(submission.getMatchId());
        messageHelper.notifySubmitterCaptain(match, submission.getSubmitterTeamId(), operatorId,
                "比赛数据审核驳回", "比赛 " + match.getMatchNo() + " 提交的数据已被驳回：" + req.getRejectReason());
    }

    @Override
    public List<MatchDataSubmissionVersionVo> listVersions(Long matchId) {
        validationService.requireMatch(matchId);
        List<MatchDataSubmissionVersionVo> versions = submissionMapper.selectVersionsByMatchId(matchId);
        for (MatchDataSubmissionVersionVo vo : versions) {
            vo.setSubmissionStatusName(MatchDataSubmissionStatusEnum.fromCode(vo.getSubmissionStatus()).getDescription());
            vo.setIsCurrent(Boolean.TRUE.equals(vo.getIsCurrent()));
        }
        return versions;
    }

    private BasketMatchDataSubmission requireSubmission(Long submissionId) {
        BasketMatchDataSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new ServiceException("提交记录不存在");
        }
        return submission;
    }

    private void assertPendingSubmission(BasketMatchDataSubmission submission) {
        if (submission.getSubmissionStatus() == null
                || submission.getSubmissionStatus() != MatchDataSubmissionStatusEnum.SUBMITTED.getCode()
                || !AUDIT_PENDING.equalsIgnoreCase(submission.getAuditStatus())) {
            throw new ServiceException("仅审核中的提交可处理");
        }
    }

    private MatchDataSubmissionDetailVo buildBaseDetail(BasketMatchDataSubmission submission, BasketMatch match) {
        MatchDataSubmissionDetailVo vo = new MatchDataSubmissionDetailVo();
        vo.setSubmissionId(submission.getId());
        vo.setSubmissionNo(submission.getSubmissionNo());
        vo.setMatchId(submission.getMatchId());
        vo.setMatchNo(match.getMatchNo());
        vo.setSeasonId(submission.getSeasonId());
        vo.setStageId(submission.getStageId());
        vo.setGroupId(submission.getGroupId());
        vo.setHomeTeamId(submission.getHomeTeamId());
        vo.setAwayTeamId(submission.getAwayTeamId());
        vo.setHomeScore(submission.getHomeScore());
        vo.setAwayScore(submission.getAwayScore());
        vo.setWinnerTeamId(submission.getWinnerTeamId());
        vo.setSubmitterTeamId(submission.getSubmitterTeamId());
        vo.setSubmitterMemberId(submission.getSubmitterMemberId());
        vo.setSubmitterPlayerId(submission.getSubmitterPlayerId());
        vo.setSubmitterRole(submission.getSubmitterRole());
        vo.setVersionNo(submission.getVersionNo());
        vo.setSubmissionStatus(submission.getSubmissionStatus());
        vo.setSubmissionStatusName(MatchDataSubmissionStatusEnum.fromCode(submission.getSubmissionStatus()).getDescription());
        vo.setAuditStatus(submission.getAuditStatus());
        vo.setSubmittedTime(submission.getSubmittedTime());
        vo.setReviewedTime(submission.getReviewedTime());
        vo.setReviewedByName(submission.getReviewedByName());
        vo.setRejectReason(submission.getRejectReason());
        vo.setAuditRemark(submission.getAuditRemark());
        vo.setRemark(submission.getRemark());
        vo.setMatchStatus(match.getMatchStatus());
        vo.setDataStatus(match.getDataStatus());
        return vo;
    }

    private List<MatchDataSubmissionPeriodVo> buildPeriodVos(List<BasketMatchDataSubmissionPeriod> periods) {
        return periods.stream().map(period -> {
            MatchDataSubmissionPeriodVo vo = new MatchDataSubmissionPeriodVo();
            vo.setPeriodNo(period.getPeriodNo());
            vo.setPeriodType(period.getPeriodType());
            vo.setPeriodTypeName(MatchPeriodTypeEnum.fromCode(period.getPeriodType()).getDescription());
            vo.setHomeScore(period.getHomeScore());
            vo.setAwayScore(period.getAwayScore());
            vo.setSortNo(period.getSortNo());
            return vo;
        }).toList();
    }

    private List<MatchDataSubmissionAuditLogVo> buildAuditLogs(Long submissionId) {
        List<MatchDataSubmissionAuditLogVo> logs = auditMapper.selectVoBySubmissionId(submissionId);
        for (MatchDataSubmissionAuditLogVo log : logs) {
            if (StringUtils.isNotEmpty(log.getAuditAction())) {
                log.setAuditActionName(MatchDataAuditActionEnum.fromCode(log.getAuditAction()).getCode());
            }
        }
        return logs;
    }

    private void fillAuditFlags(MatchDataSubmissionDetailVo vo, BasketMatchDataSubmission submission, BasketMatch match) {
        boolean canAudit = submission.getSubmissionStatus() != null
                && submission.getSubmissionStatus() == MatchDataSubmissionStatusEnum.SUBMITTED.getCode()
                && AUDIT_PENDING.equalsIgnoreCase(submission.getAuditStatus())
                && match.getMatchStatus() != null
                && match.getMatchStatus() == MatchStatusEnum.FINISHED.getCode();
        vo.setCanAudit(canAudit);
        if (!canAudit) {
            if (submission.getSubmissionStatus() != null
                    && submission.getSubmissionStatus() == MatchDataSubmissionStatusEnum.APPROVED.getCode()) {
                vo.setCannotAuditReason("提交已审核通过");
            } else if (submission.getSubmissionStatus() != null
                    && submission.getSubmissionStatus() == MatchDataSubmissionStatusEnum.REJECTED.getCode()) {
                vo.setCannotAuditReason("提交已驳回");
            } else {
                vo.setCannotAuditReason("当前状态不可审核");
            }
        }
    }

    private void writeAuditLog(BasketMatchDataSubmission submission, MatchDataAuditActionEnum action,
            String beforeStatus, String afterStatus, String auditStatus, Long auditorId, String auditorName,
            String auditRemark, String rejectReason) {
        auditLogSupport.writeAuditLog(submission.getMatchId(), submission.getId(), submission.getVersionNo(),
                action, beforeStatus, afterStatus, submission.getSubmitterMemberId(), submission.getSubmittedTime(),
                auditStatus, auditorId, auditorName, new Date(), auditRemark, rejectReason, null);
    }
}
