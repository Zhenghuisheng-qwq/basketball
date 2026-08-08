package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.*;
import com.zhs.basket.dto.v4.BasketMatchSubmissionRejectReq;
import com.zhs.basket.enums.v4.SubmissionStatusEnum;
import com.zhs.basket.mapper.v4.*;
import com.zhs.basket.query.v4.BasketMatchSubmissionAuditQuery;
import com.zhs.basket.query.v4.BasketV4MatchQuery;
import com.zhs.basket.service.v4.IBasketMatchSeriesDisplayService;
import com.zhs.basket.service.v4.IBasketV4MatchSubmissionAuditService;
import com.zhs.basket.service.v4.IBasketV4StatsSettlementService;
import com.zhs.basket.vo.v4.*;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class BasketV4MatchSubmissionAuditServiceImpl implements IBasketV4MatchSubmissionAuditService {

    @Autowired
    private BasketMatchSubmissionMapper submissionMapper;
    @Autowired
    private BasketMatchSubmissionTeamStatMapper submissionTeamStatMapper;
    @Autowired
    private BasketMatchSubmissionPlayerStatMapper submissionPlayerStatMapper;
    @Autowired
    private BasketMatchSubmissionMaterialMapper submissionMaterialMapper;
    @Autowired
    private BasketV4MatchMapper matchMapper;
    @Autowired
    private BasketEventTeamMapper eventTeamMapper;
    @Autowired
    private BasketEventPlayerMapper eventPlayerMapper;
    @Autowired
    private IBasketV4StatsSettlementService statsSettlementService;
    @Autowired
    private IBasketMatchSeriesDisplayService matchSeriesDisplayService;

    @Override
    public List<BasketMatchSubmissionAuditListVo> selectList(BasketMatchSubmissionAuditQuery query) {
        return submissionMapper.selectAuditVoList(query);
    }

    @Override
    public BasketMatchSubmissionAuditDetailVo selectDetail(Long submissionId) {
        BasketMatchSubmission submission = requireSubmission(submissionId);
        BasketV4MatchQuery matchQuery = new BasketV4MatchQuery();
        matchQuery.setId(submission.getMatchId());
        BasketMatchSubmissionAuditDetailVo vo = new BasketMatchSubmissionAuditDetailVo();
        vo.setSubmissionId(submission.getId());
        vo.setMatchId(submission.getMatchId());
        vo.setSubmitEventTeamId(submission.getSubmitEventTeamId());
        vo.setSubmitEventPlayerId(submission.getSubmitEventPlayerId());
        vo.setSubmitterRole(submission.getSubmitterRole());
        vo.setVersionNo(submission.getVersionNo());
        vo.setSubmissionStatus(submission.getSubmissionStatus());
        vo.setHomeScore(submission.getHomeScore());
        vo.setAwayScore(submission.getAwayScore());
        vo.setSubmitRemark(submission.getSubmitRemark());
        vo.setSubmitTime(submission.getSubmitTime());
        vo.setAuditUserId(submission.getAuditUserId());
        vo.setAuditUserName(submission.getAuditUserName());
        vo.setAuditTime(submission.getAuditTime());
        vo.setAuditRemark(submission.getAuditRemark());
        vo.setRejectReason(submission.getRejectReason());

        matchMapper.selectVoList(matchQuery).stream().findFirst().ifPresent(m -> {
            vo.setMatchNo(m.getMatchNo());
            vo.setRoundNo(m.getRoundNo());
            vo.setMatchTime(m.getMatchTime());
            vo.setDivisionName(m.getDivisionName());
            vo.setPlatformCode(m.getPlatformCode());
            vo.setHomeEventTeamId(m.getHomeEventTeamId());
            vo.setHomeTeamName(m.getHomeTeamName());
            vo.setAwayEventTeamId(m.getAwayEventTeamId());
            vo.setAwayTeamName(m.getAwayTeamName());
        });
        fillTeamLogos(vo);
        fillSubmitterName(vo, submission);

        BasketMatchSubmissionTeamStat teamStat = submissionTeamStatMapper.selectBySubmissionId(submissionId);
        if (teamStat != null) {
            BasketMatchSubmissionTeamStatVo tsVo = new BasketMatchSubmissionTeamStatVo();
            BeanUtils.copyProperties(teamStat, tsVo);
            vo.setTeamStat(tsVo);
        }
        List<BasketMatchSubmissionPlayerStatVo> playerStats = new ArrayList<>();
        for (BasketMatchSubmissionPlayerStat ps : submissionPlayerStatMapper.selectBySubmissionId(submissionId)) {
            BasketMatchSubmissionPlayerStatVo psVo = new BasketMatchSubmissionPlayerStatVo();
            BeanUtils.copyProperties(ps, psVo);
            BasketEventPlayer ep = eventPlayerMapper.selectById(ps.getEventPlayerId());
            if (ep != null) {
                psVo.setPlayerName(ep.getPlayerNameSnapshot());
            }
            playerStats.add(psVo);
        }
        vo.setPlayerStats(playerStats);

        List<BasketMatchSubmissionMaterialVo> materials = new ArrayList<>();
        for (BasketMatchSubmissionMaterial m : submissionMaterialMapper.selectBySubmissionId(submissionId)) {
            BasketMatchSubmissionMaterialVo mVo = new BasketMatchSubmissionMaterialVo();
            BeanUtils.copyProperties(m, mVo);
            materials.add(mVo);
        }
        vo.setMaterials(materials);

        List<BasketMatchSubmissionVersionVo> versions = new ArrayList<>();
        for (BasketMatchSubmission hist : submissionMapper.selectVersionsByMatchAndTeam(submission.getMatchId(), submission.getSubmitEventTeamId())) {
            BasketMatchSubmissionVersionVo v = new BasketMatchSubmissionVersionVo();
            v.setSubmissionId(hist.getId());
            v.setVersionNo(hist.getVersionNo());
            v.setSubmissionStatus(hist.getSubmissionStatus());
            v.setSubmitTime(hist.getSubmitTime());
            v.setAuditTime(hist.getAuditTime());
            versions.add(v);
        }
        vo.setHistoryVersions(versions);
        vo.setSeriesContext(matchSeriesDisplayService.getContextByMatchId(submission.getMatchId()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long submissionId, String auditRemark) {
        BasketMatchSubmission submission = requireSubmission(submissionId);
        if (!SubmissionStatusEnum.PENDING_REVIEW.getCode().equals(submission.getSubmissionStatus())) {
            throw new ServiceException("仅待审核状态可通过");
        }
        Date now = new Date();
        submission.setSubmissionStatus(SubmissionStatusEnum.APPROVED.getCode());
        submission.setAuditUserId(SecurityUtils.getUserId());
        submission.setAuditUserName(SecurityUtils.getUsername());
        submission.setAuditTime(now);
        submission.setAuditRemark(auditRemark);
        submission.setUpdateTime(now);
        if (submissionMapper.updateById(submission) != 1) {
            throw new ServiceException("审核通过失败");
        }
        statsSettlementService.settleBySubmission(submissionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long submissionId, BasketMatchSubmissionRejectReq req) {
        if (req == null || StringUtils.isEmpty(req.getRejectReason())) {
            throw new ServiceException("驳回原因不能为空");
        }
        BasketMatchSubmission submission = requireSubmission(submissionId);
        if (!SubmissionStatusEnum.PENDING_REVIEW.getCode().equals(submission.getSubmissionStatus())) {
            throw new ServiceException("仅待审核状态可驳回");
        }
        Date now = new Date();
        submission.setSubmissionStatus(SubmissionStatusEnum.REJECTED.getCode());
        submission.setRejectReason(req.getRejectReason().trim());
        submission.setAuditRemark(req.getAuditRemark());
        submission.setAuditUserId(SecurityUtils.getUserId());
        submission.setAuditUserName(SecurityUtils.getUsername());
        submission.setAuditTime(now);
        submission.setUpdateTime(now);
        if (submissionMapper.updateById(submission) != 1) {
            throw new ServiceException("审核驳回失败");
        }
    }

    private void fillTeamLogos(BasketMatchSubmissionAuditDetailVo vo) {
        if (vo.getHomeEventTeamId() != null) {
            BasketEventTeam ht = eventTeamMapper.selectById(vo.getHomeEventTeamId());
            if (ht != null) {
                vo.setHomeTeamLogoFileId(ht.getTeamLogoFileId());
            }
        }
        if (vo.getAwayEventTeamId() != null) {
            BasketEventTeam at = eventTeamMapper.selectById(vo.getAwayEventTeamId());
            if (at != null) {
                vo.setAwayTeamLogoFileId(at.getTeamLogoFileId());
            }
        }
        if (vo.getSubmitEventTeamId() != null) {
            BasketEventTeam st = eventTeamMapper.selectById(vo.getSubmitEventTeamId());
            if (st != null) {
                vo.setSubmitTeamName(st.getTeamNameSnapshot());
                vo.setSubmitTeamLogoFileId(st.getTeamLogoFileId());
            }
        }
    }

    private void fillSubmitterName(BasketMatchSubmissionAuditDetailVo vo, BasketMatchSubmission submission) {
        if (submission.getSubmitEventPlayerId() != null) {
            BasketEventPlayer ep = eventPlayerMapper.selectById(submission.getSubmitEventPlayerId());
            if (ep != null) {
                vo.setSubmitterName(ep.getPlayerNameSnapshot());
            }
        }
    }

    private BasketMatchSubmission requireSubmission(Long id) {
        BasketMatchSubmission submission = submissionMapper.selectById(id);
        if (submission == null) {
            throw new ServiceException("提交不存在");
        }
        return submission;
    }
}
