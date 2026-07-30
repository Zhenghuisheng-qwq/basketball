package com.zhs.basket.service.impl;

import com.zhs.basket.constant.BasketMatchConstants;
import com.zhs.basket.domain.BasketMatch;
import com.zhs.basket.domain.BasketMatchExceptionApplication;
import com.zhs.basket.domain.BasketMatchExceptionAudit;
import com.zhs.basket.enums.MatchChangeTypeEnum;
import com.zhs.basket.enums.MatchDataStatusEnum;
import com.zhs.basket.enums.MatchResultTypeEnum;
import com.zhs.basket.enums.MatchStatusEnum;
import com.zhs.basket.mapper.BasketMatchExceptionApplicationMapper;
import com.zhs.basket.mapper.BasketMatchExceptionAuditMapper;
import com.zhs.basket.mapper.BasketMatchExceptionMaterialMapper;
import com.zhs.basket.mapper.BasketMatchMapper;
import com.zhs.basket.mapper.BasketMatchTeamMapper;
import com.zhs.basket.query.MatchExceptionQuery;
import com.zhs.basket.req.MatchExceptionDecideReq;
import com.zhs.basket.req.MatchOfficialDataCorrectionReq;
import com.zhs.basket.service.IBasketDataSettlementService;
import com.zhs.basket.service.IBasketMatchExceptionService;
import com.zhs.basket.service.IBasketMatchOfficialDataCorrectionService;
import com.zhs.basket.service.IBasketMatchOfficialResultService;
import com.zhs.basket.service.IBasketMatchStatSyncService;
import com.zhs.basket.service.IBasketMatchValidationService;
import com.zhs.basket.support.BasketMatchMessageHelper;
import com.zhs.basket.vo.MatchExceptionDetailVo;
import com.zhs.basket.vo.MatchExceptionListVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;

@Service
public class BasketMatchExceptionServiceImpl implements IBasketMatchExceptionService {

    private static final Integer NOT_DELETED = 0;
    private static final String SOURCE_EXCEPTION = "EXCEPTION_DECISION";

    @Autowired private BasketMatchExceptionApplicationMapper applicationMapper;
    @Autowired private BasketMatchExceptionMaterialMapper materialMapper;
    @Autowired private BasketMatchExceptionAuditMapper auditMapper;
    @Autowired private BasketMatchMapper matchMapper;
    @Autowired private BasketMatchTeamMapper matchTeamMapper;
    @Autowired private IBasketMatchValidationService validationService;
    @Autowired private IBasketMatchOfficialDataCorrectionService correctionService;
    @Autowired private IBasketMatchOfficialResultService officialResultService;
    @Autowired private IBasketMatchStatSyncService statSyncService;
    @Lazy @Autowired private IBasketDataSettlementService dataSettlementService;
    @Autowired private BasketMatchMessageHelper messageHelper;

    @Override
    public java.util.List<MatchExceptionListVo> list(MatchExceptionQuery query) {
        return applicationMapper.selectList(query != null ? query : new MatchExceptionQuery());
    }

    @Override
    public MatchExceptionDetailVo getDetail(Long applicationId) {
        BasketMatchExceptionApplication app = requireApplication(applicationId);
        MatchExceptionDetailVo vo = new MatchExceptionDetailVo();
        vo.setApplication(app);
        vo.setMaterials(materialMapper.selectVoByApplicationId(applicationId));
        vo.setAudits(auditMapper.selectByApplicationId(applicationId));
        BasketMatch match = matchMapper.selectById(app.getMatchId());
        if (match != null) {
            vo.setMatchNo(match.getMatchNo());
            vo.setHomeTeamId(match.getHomeTeamId());
            vo.setAwayTeamId(match.getAwayTeamId());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long applicationId, String rejectReason, String auditRemark) {
        BasketMatchExceptionApplication app = requireApplication(applicationId);
        Long operatorId = SecurityUtils.getUserId();
        String operatorName = SecurityUtils.getUsername();
        if (applicationMapper.updateDecision(applicationId, "PENDING", "REJECTED", "REJECT",
                auditRemark, null, null, null, null, operatorId, operatorName) != 1) {
            throw new ServiceException("异常申请驳回失败，可能已被处理");
        }
        writeAudit(app, "REJECT", "PENDING", "REJECTED", operatorId, operatorName, auditRemark, rejectReason);
        notifyCaptainsSafe(app.getMatchId(), operatorId, "异常申请已驳回",
                StringUtils.isNotEmpty(rejectReason) ? rejectReason : auditRemark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decide(Long applicationId, MatchExceptionDecideReq req) {
        if (req == null || StringUtils.isEmpty(req.getDecisionType())) {
            throw new ServiceException("裁定类型不能为空");
        }
        BasketMatchExceptionApplication app = requireApplication(applicationId);
        BasketMatch match = validationService.requireMatch(app.getMatchId());
        Long operatorId = SecurityUtils.getUserId();
        String operatorName = SecurityUtils.getUsername();
        String decisionType = req.getDecisionType().toUpperCase();

        switch (decisionType) {
            case "REJECT" -> reject(applicationId, req.getAuditRemark(), req.getAuditRemark());
            case "FORFEIT" -> applyForfeit(app, match, req, operatorId, operatorName);
            case "POSTPONE" -> applyPostpone(app, match, req, operatorId, operatorName);
            case "CANCEL" -> applyCancel(app, match, req, operatorId, operatorName);
            case "REMATCH" -> applyRematch(app, match, req, operatorId, operatorName);
            case "KEEP_RESULT" -> applyKeepResult(app, match, req, operatorId, operatorName);
            case "ADMIN_DECISION" -> applyAdminDecision(app, match, req, operatorId, operatorName);
            default -> throw new ServiceException("不支持的裁定类型：" + decisionType);
        }
    }

    private void applyForfeit(BasketMatchExceptionApplication app, BasketMatch match,
                              MatchExceptionDecideReq req, Long operatorId, String operatorName) {
        Long walkoverTeamId = req.getWalkoverTeamId();
        if (walkoverTeamId == null) {
            throw new ServiceException("弃赛方不能为空");
        }
        if (!walkoverTeamId.equals(match.getHomeTeamId()) && !walkoverTeamId.equals(match.getAwayTeamId())) {
            throw new ServiceException("弃赛方必须是参赛球队");
        }
        Long winnerTeamId = walkoverTeamId.equals(match.getHomeTeamId()) ? match.getAwayTeamId() : match.getHomeTeamId();
        int homeScore = walkoverTeamId.equals(match.getHomeTeamId())
                ? BasketMatchConstants.WALKOVER_LOSS_SCORE : BasketMatchConstants.WALKOVER_WIN_SCORE;
        int awayScore = walkoverTeamId.equals(match.getAwayTeamId())
                ? BasketMatchConstants.WALKOVER_LOSS_SCORE : BasketMatchConstants.WALKOVER_WIN_SCORE;

        Date now = new Date();
        matchMapper.updateScores(match.getId(), homeScore, awayScore, winnerTeamId, 0, walkoverTeamId,
                MatchStatusEnum.FINISHED.getCode(), MatchDataStatusEnum.CONFIRMED.getCode(), now);
        matchTeamMapper.updateScoreAndResult(match.getId(), winnerTeamId, Math.max(homeScore, awayScore),
                MatchResultTypeEnum.FORFEIT_WIN.getCode(), 1);
        matchTeamMapper.updateScoreAndResult(match.getId(), walkoverTeamId, Math.min(homeScore, awayScore),
                MatchResultTypeEnum.FORFEIT_LOSS.getCode(), 0);

        finalizeDecision(app, req, operatorId, operatorName, homeScore, awayScore, winnerTeamId, walkoverTeamId);
        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);
        match.setWinnerTeamId(winnerTeamId);
        match.setDataStatus(MatchDataStatusEnum.CONFIRMED.getCode());
        validationService.writeStatusLog(match, match.getMatchStatus(), MatchChangeTypeEnum.FINISH,
                "异常裁定弃赛，默认比分 " + BasketMatchConstants.WALKOVER_WIN_SCORE + "-"
                        + BasketMatchConstants.WALKOVER_LOSS_SCORE);
        var officialResult = officialResultService.publishFromMatch(match, operatorId);
        if (statSyncService.tryRecordSync(officialResult, SOURCE_EXCEPTION + "_FORFEIT")) {
            dataSettlementService.rebuildMatch(match.getId());
        }
        notifyCaptainsSafe(match.getId(), operatorId, "异常比赛已裁定弃赛", req.getDecisionResult());
    }

    private void applyPostpone(BasketMatchExceptionApplication app, BasketMatch match,
                               MatchExceptionDecideReq req, Long operatorId, String operatorName) {
        if (matchMapper.updateStatusIfMatch(match.getId(),
                Collections.singletonList(match.getMatchStatus()), MatchStatusEnum.POSTPONED.getCode()) != 1) {
            matchMapper.updateScores(match.getId(), match.getHomeScore(), match.getAwayScore(), match.getWinnerTeamId(),
                    0, null, MatchStatusEnum.POSTPONED.getCode(), match.getDataStatus(), new Date());
        }
        finalizeDecision(app, req, operatorId, operatorName, null, null, null, null);
        validationService.writeStatusLog(match, match.getMatchStatus(), MatchChangeTypeEnum.POSTPONE,
                req.getDecisionResult());
        notifyCaptainsSafe(match.getId(), operatorId, "比赛已延期", req.getDecisionResult());
    }

    private void applyCancel(BasketMatchExceptionApplication app, BasketMatch match,
                             MatchExceptionDecideReq req, Long operatorId, String operatorName) {
        matchMapper.updateScores(match.getId(), match.getHomeScore(), match.getAwayScore(), match.getWinnerTeamId(),
                0, null, MatchStatusEnum.CANCELLED.getCode(), match.getDataStatus(), new Date());
        finalizeDecision(app, req, operatorId, operatorName, null, null, null, null);
        validationService.writeStatusLog(match, match.getMatchStatus(), MatchChangeTypeEnum.CANCEL,
                req.getDecisionResult());
        notifyCaptainsSafe(match.getId(), operatorId, "比赛已取消", req.getDecisionResult());
    }

    private void applyRematch(BasketMatchExceptionApplication app, BasketMatch match,
                              MatchExceptionDecideReq req, Long operatorId, String operatorName) {
        finalizeDecision(app, req, operatorId, operatorName, null, null, null, null);
        validationService.writeStatusLog(match, match.getMatchStatus(), MatchChangeTypeEnum.REOPEN,
                "异常裁定需重赛：" + req.getDecisionResult());
        notifyCaptainsSafe(match.getId(), operatorId, "比赛需重赛", req.getDecisionResult());
    }

    private void applyKeepResult(BasketMatchExceptionApplication app, BasketMatch match,
                                 MatchExceptionDecideReq req, Long operatorId, String operatorName) {
        finalizeDecision(app, req, operatorId, operatorName, match.getHomeScore(), match.getAwayScore(),
                match.getWinnerTeamId(), null);
        validationService.writeStatusLog(match, match.getMatchStatus(), MatchChangeTypeEnum.FINISH,
                "异常申请维持原结果：" + req.getDecisionResult());
        notifyCaptainsSafe(match.getId(), operatorId, "异常申请维持原结果", req.getDecisionResult());
    }

    private void applyAdminDecision(BasketMatchExceptionApplication app, BasketMatch match,
                                    MatchExceptionDecideReq req, Long operatorId, String operatorName) {
        MatchOfficialDataCorrectionReq correction = req.getCorrectionData();
        if (correction == null) {
            throw new ServiceException("管理员裁定须提供修正数据");
        }
        correctionService.applyCorrection(match.getId(), correction, SOURCE_EXCEPTION, operatorId, null,
                req.getDecisionResult());
        finalizeDecision(app, req, operatorId, operatorName, correction.getHomeScore(), correction.getAwayScore(),
                req.getWinnerTeamId(), req.getWalkoverTeamId());
        notifyCaptainsSafe(match.getId(), operatorId, "异常比赛已管理员裁定", req.getDecisionResult());
    }

    private void finalizeDecision(BasketMatchExceptionApplication app, MatchExceptionDecideReq req,
                                  Long operatorId, String operatorName,
                                  Integer homeScore, Integer awayScore, Long winnerTeamId, Long walkoverTeamId) {
        if (applicationMapper.updateDecision(app.getId(), "PENDING", "APPROVED", req.getDecisionType(),
                req.getDecisionResult(), homeScore, awayScore, winnerTeamId, walkoverTeamId,
                operatorId, operatorName) != 1) {
            throw new ServiceException("异常裁定失败，可能已被处理");
        }
        writeAudit(app, "DECIDE", "PENDING", "APPROVED", operatorId, operatorName, req.getAuditRemark(), null);
    }

    private BasketMatchExceptionApplication requireApplication(Long applicationId) {
        BasketMatchExceptionApplication app = applicationMapper.selectById(applicationId);
        if (app == null) {
            throw new ServiceException("异常申请不存在");
        }
        return app;
    }

    private void writeAudit(BasketMatchExceptionApplication app, String action, String before, String after,
                            Long operatorId, String operatorName, String remark, String rejectReason) {
        Date now = new Date();
        BasketMatchExceptionAudit audit = new BasketMatchExceptionAudit();
        audit.setApplicationId(app.getId());
        audit.setMatchId(app.getMatchId());
        audit.setAuditAction(action);
        audit.setBeforeStatus(before);
        audit.setAfterStatus(after);
        audit.setOperatorId(operatorId);
        audit.setOperatorName(operatorName);
        audit.setAuditRemark(remark);
        audit.setRejectReason(rejectReason);
        audit.setCreateTime(now);
        audit.setUpdateTime(now);
        audit.setIsDelete(NOT_DELETED);
        auditMapper.insert(audit);
    }

    private void notifyCaptainsSafe(Long matchId, Long operatorId, String title, String content) {
        BasketMatch match = matchMapper.selectById(matchId);
        if (match == null) {
            return;
        }
        try {
            messageHelper.notifyCaptains(match, operatorId, title, content);
        } catch (Exception ignored) {
        }
    }
}
