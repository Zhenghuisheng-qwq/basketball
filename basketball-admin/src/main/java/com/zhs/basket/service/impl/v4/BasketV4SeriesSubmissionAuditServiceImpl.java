package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketEventPlayer;
import com.zhs.basket.domain.v4.BasketEventTeam;
import com.zhs.basket.domain.v4.BasketMatchSeries;
import com.zhs.basket.domain.v4.BasketMatchSeriesSubmission;
import com.zhs.basket.domain.v4.BasketMatchSubmission;
import com.zhs.basket.domain.v4.BasketMatchSubmissionMaterial;
import com.zhs.basket.domain.v4.BasketMatchSubmissionPeriodScore;
import com.zhs.basket.domain.v4.BasketMatchSubmissionPlayerStat;
import com.zhs.basket.domain.v4.BasketMatchSubmissionTeamStat;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.dto.v4.BasketMatchSubmissionRejectReq;
import com.zhs.basket.enums.v4.SubmissionStatusEnum;
import com.zhs.basket.mapper.v4.BasketEventPlayerMapper;
import com.zhs.basket.mapper.v4.BasketEventTeamMapper;
import com.zhs.basket.mapper.v4.BasketMatchSeriesMapper;
import com.zhs.basket.mapper.v4.BasketMatchSeriesSubmissionMapper;
import com.zhs.basket.mapper.v4.BasketMatchSubmissionMapper;
import com.zhs.basket.mapper.v4.BasketMatchSubmissionMaterialMapper;
import com.zhs.basket.mapper.v4.BasketMatchSubmissionPeriodScoreMapper;
import com.zhs.basket.mapper.v4.BasketMatchSubmissionPlayerStatMapper;
import com.zhs.basket.mapper.v4.BasketMatchSubmissionTeamStatMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.query.v4.BasketSeriesSubmissionAuditQuery;
import com.zhs.basket.service.v4.IBasketMatchSeriesService;
import com.zhs.basket.service.v4.IBasketV4SeriesSubmissionAuditService;
import com.zhs.basket.service.v4.IBasketV4StatsSettlementService;
import com.zhs.basket.vo.v4.BasketMatchSubmissionMaterialVo;
import com.zhs.basket.vo.v4.BasketMatchSubmissionPeriodScoreVo;
import com.zhs.basket.vo.v4.BasketMatchSubmissionPlayerStatVo;
import com.zhs.basket.vo.v4.BasketMatchSubmissionTeamStatVo;
import com.zhs.basket.vo.v4.BasketMatchSubmissionVersionVo;
import com.zhs.basket.vo.v4.BasketSeriesSubmissionAuditDetailVo;
import com.zhs.basket.vo.v4.BasketSeriesSubmissionAuditListVo;
import com.zhs.basket.vo.v4.BasketSeriesSubmissionGameAuditVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Service
public class BasketV4SeriesSubmissionAuditServiceImpl implements IBasketV4SeriesSubmissionAuditService {

    @Autowired
    private BasketMatchSeriesSubmissionMapper seriesSubmissionMapper;
    @Autowired
    private BasketMatchSeriesMapper matchSeriesMapper;
    @Autowired
    private BasketMatchSubmissionMapper submissionMapper;
    @Autowired
    private BasketMatchSubmissionTeamStatMapper submissionTeamStatMapper;
    @Autowired
    private BasketMatchSubmissionPlayerStatMapper submissionPlayerStatMapper;
    @Autowired
    private BasketMatchSubmissionPeriodScoreMapper submissionPeriodScoreMapper;
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
    private IBasketMatchSeriesService matchSeriesService;

    @Override
    public List<BasketSeriesSubmissionAuditListVo> selectList(BasketSeriesSubmissionAuditQuery query) {
        return seriesSubmissionMapper.selectAuditVoList(query);
    }

    @Override
    public BasketSeriesSubmissionAuditDetailVo selectDetail(Long seriesSubmissionId) {
        BasketMatchSeriesSubmission header = requireHeader(seriesSubmissionId);
        BasketMatchSeries series = matchSeriesMapper.selectById(header.getMatchSeriesId());
        BasketSeriesSubmissionAuditDetailVo vo = new BasketSeriesSubmissionAuditDetailVo();
        vo.setSeriesSubmissionId(header.getId());
        vo.setMatchSeriesId(header.getMatchSeriesId());
        vo.setDeclaredHomeWinCount(header.getDeclaredHomeWinCount());
        vo.setDeclaredAwayWinCount(header.getDeclaredAwayWinCount());
        vo.setActualGameCount(header.getActualGameCount());
        vo.setVersionNo(header.getVersionNo());
        vo.setStatus(header.getStatus());
        vo.setSubmitTime(header.getSubmitTime());
        vo.setRejectReason(header.getRejectReason());
        vo.setAuditRemark(header.getAuditRemark());
        vo.setAuditTime(header.getAuditTime());
        vo.setAuditUserName(header.getAuditUserName());
        vo.setSubmitEventTeamId(header.getSubmitEventTeamId());
        if (series != null) {
            vo.setSeriesCode(series.getSeriesCode());
            vo.setBestOf(series.getBestOf());
            vo.setRoundNo(series.getRoundNo());
            vo.setHomeEventTeamId(series.getHomeEventTeamId());
            vo.setAwayEventTeamId(series.getAwayEventTeamId());
            vo.setHomeTeamName(teamName(series.getHomeEventTeamId()));
            vo.setAwayTeamName(teamName(series.getAwayEventTeamId()));
        }
        vo.setSubmitTeamName(teamName(header.getSubmitEventTeamId()));
        if (header.getDeclaredHomeWinCount() != null && header.getDeclaredAwayWinCount() != null) {
            vo.setDeclaredWinnerTeamName(header.getDeclaredHomeWinCount() > header.getDeclaredAwayWinCount()
                    ? vo.getHomeTeamName() : vo.getAwayTeamName());
        }
        List<BasketMatchSubmission> games = submissionMapper.selectBySeriesSubmissionId(header.getId());
        games.sort(Comparator.comparing(s -> {
            BasketV4Match match = matchMapper.selectById(s.getMatchId());
            return match == null || match.getSeriesGameNo() == null ? 0 : match.getSeriesGameNo();
        }));
        List<BasketSeriesSubmissionGameAuditVo> gameVos = new ArrayList<>();
        for (BasketMatchSubmission submission : games) {
            gameVos.add(toGameVo(submission, series));
        }
        vo.setGames(gameVos);

        List<BasketMatchSubmissionVersionVo> versions = new ArrayList<>();
        for (BasketMatchSeriesSubmission hist : seriesSubmissionMapper.selectVersionsBySeriesId(header.getMatchSeriesId())) {
            BasketMatchSubmissionVersionVo v = new BasketMatchSubmissionVersionVo();
            v.setSubmissionId(hist.getId());
            v.setVersionNo(hist.getVersionNo());
            v.setSubmissionStatus(hist.getStatus());
            v.setSubmitTime(hist.getSubmitTime());
            v.setAuditTime(hist.getAuditTime());
            versions.add(v);
        }
        vo.setHistoryVersions(versions);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long seriesSubmissionId, String auditRemark) {
        BasketMatchSeriesSubmission header = seriesSubmissionMapper.selectByIdForUpdate(seriesSubmissionId);
        if (header == null) {
            throw new ServiceException("系列赛提交不存在");
        }
        if (SubmissionStatusEnum.APPROVED.getCode().equals(header.getStatus())) {
            return;
        }
        if (!SubmissionStatusEnum.PENDING_REVIEW.getCode().equals(header.getStatus())) {
            throw new ServiceException("仅待审核状态可通过");
        }
        BasketMatchSeries series = matchSeriesMapper.selectById(header.getMatchSeriesId());
        if (series == null) {
            throw new ServiceException("系列赛不存在");
        }
        List<BasketMatchSubmission> games = submissionMapper.selectBySeriesSubmissionId(header.getId());
        games.sort(Comparator.comparing(s -> {
            BasketV4Match match = matchMapper.selectById(s.getMatchId());
            return match == null || match.getSeriesGameNo() == null ? 0 : match.getSeriesGameNo();
        }));
        Date now = new Date();
        for (BasketMatchSubmission submission : games) {
            if (!Integer.valueOf(1).equals(submission.getDraftComplete())) {
                continue;
            }
            statsSettlementService.settleBySubmission(submission.getId());
            submission.setSubmissionStatus(SubmissionStatusEnum.APPROVED.getCode());
            submission.setAuditUserId(SecurityUtils.getUserId());
            submission.setAuditUserName(SecurityUtils.getUsername());
            submission.setAuditTime(now);
            submission.setAuditRemark(auditRemark);
            submission.setUpdateTime(now);
            submissionMapper.updateById(submission);
        }
        matchSeriesService.rebuildSeriesResult(series.getId());
        BasketMatchSeries official = matchSeriesMapper.selectById(series.getId());
        if (official == null
                || !sameInt(official.getHomeWinCount(), header.getDeclaredHomeWinCount())
                || !sameInt(official.getAwayWinCount(), header.getDeclaredAwayWinCount())) {
            throw new ServiceException("正式系列赛比分与声明比分不一致，已阻止审核通过");
        }
        header.setStatus(SubmissionStatusEnum.APPROVED.getCode());
        header.setAuditUserId(SecurityUtils.getUserId());
        header.setAuditUserName(SecurityUtils.getUsername());
        header.setAuditTime(now);
        header.setAuditRemark(auditRemark);
        header.setUpdateTime(now);
        if (seriesSubmissionMapper.updateById(header) != 1) {
            throw new ServiceException("系列赛审核通过失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long seriesSubmissionId, BasketMatchSubmissionRejectReq req) {
        if (req == null || StringUtils.isEmpty(req.getRejectReason())) {
            throw new ServiceException("驳回原因不能为空");
        }
        BasketMatchSeriesSubmission header = seriesSubmissionMapper.selectByIdForUpdate(seriesSubmissionId);
        if (header == null) {
            throw new ServiceException("系列赛提交不存在");
        }
        if (!SubmissionStatusEnum.PENDING_REVIEW.getCode().equals(header.getStatus())) {
            throw new ServiceException("仅待审核状态可驳回");
        }
        Date now = new Date();
        header.setStatus(SubmissionStatusEnum.REJECTED.getCode());
        header.setRejectReason(req.getRejectReason().trim());
        header.setAuditRemark(req.getAuditRemark());
        header.setAuditUserId(SecurityUtils.getUserId());
        header.setAuditUserName(SecurityUtils.getUsername());
        header.setAuditTime(now);
        header.setUpdateTime(now);
        if (seriesSubmissionMapper.updateById(header) != 1) {
            throw new ServiceException("系列赛审核驳回失败");
        }
        for (BasketMatchSubmission submission : submissionMapper.selectBySeriesSubmissionId(header.getId())) {
            submission.setSubmissionStatus(SubmissionStatusEnum.DRAFT.getCode());
            submission.setRejectReason(req.getRejectReason().trim());
            submission.setUpdateTime(now);
            submissionMapper.updateById(submission);
        }
    }

    private BasketSeriesSubmissionGameAuditVo toGameVo(BasketMatchSubmission submission, BasketMatchSeries series) {
        BasketV4Match match = matchMapper.selectById(submission.getMatchId());
        BasketSeriesSubmissionGameAuditVo vo = new BasketSeriesSubmissionGameAuditVo();
        vo.setMatchId(submission.getMatchId());
        vo.setSubmissionId(submission.getId());
        vo.setHomeScore(submission.getHomeScore());
        vo.setAwayScore(submission.getAwayScore());
        vo.setSubmissionStatus(submission.getSubmissionStatus());
        vo.setDraftComplete(submission.getDraftComplete());
        vo.setSubmitRemark(submission.getSubmitRemark());
        if (match != null) {
            vo.setSeriesGameNo(match.getSeriesGameNo());
            vo.setMatchNo(match.getMatchNo());
        }
        Long homeId = match != null ? match.getHomeEventTeamId() : (series == null ? null : series.getHomeEventTeamId());
        Long awayId = match != null ? match.getAwayEventTeamId() : (series == null ? null : series.getAwayEventTeamId());
        for (BasketMatchSubmissionTeamStat ts : submissionTeamStatMapper.selectListBySubmissionId(submission.getId())) {
            BasketMatchSubmissionTeamStatVo tsVo = new BasketMatchSubmissionTeamStatVo();
            BeanUtils.copyProperties(ts, tsVo);
            if (ts.getEventTeamId().equals(homeId)) {
                vo.setHomeTeamStat(tsVo);
            } else if (ts.getEventTeamId().equals(awayId)) {
                vo.setAwayTeamStat(tsVo);
            }
        }
        List<BasketMatchSubmissionPlayerStatVo> homePlayers = new ArrayList<>();
        List<BasketMatchSubmissionPlayerStatVo> awayPlayers = new ArrayList<>();
        for (BasketMatchSubmissionPlayerStat ps : submissionPlayerStatMapper.selectBySubmissionId(submission.getId())) {
            BasketMatchSubmissionPlayerStatVo psVo = new BasketMatchSubmissionPlayerStatVo();
            BeanUtils.copyProperties(ps, psVo);
            BasketEventPlayer ep = eventPlayerMapper.selectById(ps.getEventPlayerId());
            if (ep != null) {
                psVo.setPlayerName(ep.getPlayerNameSnapshot());
            }
            if (ps.getEventTeamId() != null && ps.getEventTeamId().equals(homeId)) {
                homePlayers.add(psVo);
            } else {
                awayPlayers.add(psVo);
            }
        }
        vo.setHomePlayerStats(homePlayers);
        vo.setAwayPlayerStats(awayPlayers);
        List<BasketMatchSubmissionPeriodScoreVo> periods = new ArrayList<>();
        for (BasketMatchSubmissionPeriodScore period : submissionPeriodScoreMapper.selectBySubmissionId(submission.getId())) {
            BasketMatchSubmissionPeriodScoreVo pVo = new BasketMatchSubmissionPeriodScoreVo();
            pVo.setPeriodNo(period.getPeriodNo());
            pVo.setHomeScore(period.getHomeScore());
            pVo.setAwayScore(period.getAwayScore());
            periods.add(pVo);
        }
        vo.setPeriodScores(periods);
        List<BasketMatchSubmissionMaterialVo> materials = new ArrayList<>();
        for (BasketMatchSubmissionMaterial m : submissionMaterialMapper.selectBySubmissionId(submission.getId())) {
            BasketMatchSubmissionMaterialVo mVo = new BasketMatchSubmissionMaterialVo();
            BeanUtils.copyProperties(m, mVo);
            if (m.getFileId() != null) {
                mVo.setAccessUrl("/basket/v4/match-submission-audit/material/" + m.getFileId());
            }
            materials.add(mVo);
        }
        vo.setMaterials(materials);
        vo.setValidationErrors(buildGameValidation(vo));
        return vo;
    }

    private List<String> buildGameValidation(BasketSeriesSubmissionGameAuditVo game) {
        List<String> errors = new ArrayList<>();
        String label = "G" + (game.getSeriesGameNo() == null ? "?" : game.getSeriesGameNo());
        int homePts = sumPlayerPoints(game.getHomePlayerStats());
        int awayPts = sumPlayerPoints(game.getAwayPlayerStats());
        int homeScore = game.getHomeScore() == null ? 0 : game.getHomeScore();
        int awayScore = game.getAwayScore() == null ? 0 : game.getAwayScore();
        if (homePts != homeScore) {
            errors.add(label + "：主队球员 PTS 合计 " + homePts + " 必须等于主队比分 " + homeScore);
        }
        if (awayPts != awayScore) {
            errors.add(label + "：客队球员 PTS 合计 " + awayPts + " 必须等于客队比分 " + awayScore);
        }
        int homeQ = 0;
        int awayQ = 0;
        if (game.getPeriodScores() != null) {
            for (BasketMatchSubmissionPeriodScoreVo period : game.getPeriodScores()) {
                homeQ += period.getHomeScore() == null ? 0 : period.getHomeScore();
                awayQ += period.getAwayScore() == null ? 0 : period.getAwayScore();
            }
        }
        if (homeQ != homeScore) {
            errors.add(label + "：节比分主队合计 " + homeQ + " 必须等于本场比分 " + homeScore);
        }
        if (awayQ != awayScore) {
            errors.add(label + "：节比分客队合计 " + awayQ + " 必须等于本场比分 " + awayScore);
        }
        if (game.getMaterials() == null || game.getMaterials().isEmpty()) {
            errors.add(label + "：请上传本场证明材料");
        }
        return errors;
    }

    private int sumPlayerPoints(List<BasketMatchSubmissionPlayerStatVo> rows) {
        if (rows == null) {
            return 0;
        }
        int sum = 0;
        for (BasketMatchSubmissionPlayerStatVo row : rows) {
            sum += row.getPoints() == null ? 0 : row.getPoints();
        }
        return sum;
    }

    private BasketMatchSeriesSubmission requireHeader(Long id) {
        BasketMatchSeriesSubmission header = seriesSubmissionMapper.selectById(id);
        if (header == null) {
            throw new ServiceException("系列赛提交不存在");
        }
        return header;
    }

    private String teamName(Long eventTeamId) {
        if (eventTeamId == null) {
            return null;
        }
        BasketEventTeam team = eventTeamMapper.selectById(eventTeamId);
        return team == null ? null : team.getTeamNameSnapshot();
    }

    private boolean sameInt(Integer a, Integer b) {
        return a == null ? b == null : a.equals(b);
    }
}
