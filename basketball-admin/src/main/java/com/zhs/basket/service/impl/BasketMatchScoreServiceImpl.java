package com.zhs.basket.service.impl;

import com.zhs.basket.constant.BasketMatchConstants;
import com.zhs.basket.domain.BasketMatch;
import com.zhs.basket.domain.BasketMatchPeriodScore;
import com.zhs.basket.enums.MatchChangeTypeEnum;
import com.zhs.basket.enums.MatchDataStatusEnum;
import com.zhs.basket.enums.MatchPeriodTypeEnum;
import com.zhs.basket.enums.MatchResultTypeEnum;
import com.zhs.basket.enums.MatchStatusEnum;
import com.zhs.basket.mapper.BasketMatchMapper;
import com.zhs.basket.mapper.BasketMatchPeriodScoreMapper;
import com.zhs.basket.mapper.BasketMatchTeamMapper;
import com.zhs.basket.req.MatchForfeitReq;
import com.zhs.basket.req.MatchPeriodScoreSaveReq;
import com.zhs.basket.req.MatchReopenReq;
import com.zhs.basket.service.IBasketMatchScoreService;
import com.zhs.basket.service.IBasketMatchValidationService;
import com.zhs.basket.vo.MatchPeriodScoreVo;
import com.zhs.basket.vo.MatchScoreVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
public class BasketMatchScoreServiceImpl implements IBasketMatchScoreService {

    private static final Integer NOT_DELETED = 0;
    private static final int PERIOD_STATUS_DRAFT = 0;

    @Autowired private IBasketMatchValidationService validationService;
    @Autowired private BasketMatchMapper matchMapper;
    @Autowired private BasketMatchPeriodScoreMapper periodScoreMapper;
    @Autowired private BasketMatchTeamMapper matchTeamMapper;

    @Override
    public MatchScoreVo selectScore(Long matchId) {
        BasketMatch match = validationService.requireMatch(matchId);
        MatchScoreVo vo = new MatchScoreVo();
        vo.setMatchId(matchId);
        vo.setHomeScore(match.getHomeScore());
        vo.setAwayScore(match.getAwayScore());
        vo.setWinnerTeamId(match.getWinnerTeamId());
        vo.setOvertimeCount(match.getOvertimeCount());
        vo.setMatchStatus(match.getMatchStatus());
        vo.setMatchStatusName(MatchStatusEnum.fromCode(match.getMatchStatus()).getDescription());
        vo.setDataStatus(match.getDataStatus());
        vo.setDataStatusName(MatchDataStatusEnum.fromCode(match.getDataStatus()).getDescription());
        List<MatchPeriodScoreVo> periods = periodScoreMapper.selectVoByMatchId(matchId);
        for (MatchPeriodScoreVo p : periods) {
            p.setPeriodTypeName(MatchPeriodTypeEnum.fromCode(p.getPeriodType()).getDescription());
        }
        vo.setPeriods(periods);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePeriodScores(Long matchId, MatchPeriodScoreSaveReq req) {
        BasketMatch match = validationService.requireMatch(matchId);
        assertScoreEditable(match);
        if (req == null || CollectionUtils.isEmpty(req.getPeriods())) {
            throw new ServiceException("分节比分不能为空");
        }
        List<BasketMatchPeriodScore> list = new ArrayList<>();
        Date now = new Date();
        for (MatchPeriodScoreSaveReq.PeriodScoreItem item : req.getPeriods()) {
            if (item.getPeriodNo() == null || StringUtils.isEmpty(item.getPeriodType())) {
                throw new ServiceException("节次信息不完整");
            }
            MatchPeriodTypeEnum.fromCode(item.getPeriodType());
            BasketMatchPeriodScore period = new BasketMatchPeriodScore();
            period.setMatchId(matchId);
            period.setPeriodNo(item.getPeriodNo());
            period.setPeriodType(item.getPeriodType().trim().toUpperCase());
            period.setHomeScore(item.getHomeScore() != null ? item.getHomeScore() : 0);
            period.setAwayScore(item.getAwayScore() != null ? item.getAwayScore() : 0);
            period.setPeriodStatus(PERIOD_STATUS_DRAFT);
            period.setCreateTime(now);
            period.setUpdateTime(now);
            period.setIsDelete(NOT_DELETED);
            list.add(period);
        }
        periodScoreMapper.softDeleteByMatchId(matchId);
        periodScoreMapper.batchInsert(list);
        validationService.writeStatusLog(match, match.getMatchStatus(), MatchChangeTypeEnum.SCORE_SAVE, "保存分节比分");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitScore(Long matchId) {
        BasketMatch match = validationService.requireMatch(matchId);
        assertScoreEditable(match);
        List<MatchPeriodScoreVo> periods = periodScoreMapper.selectVoByMatchId(matchId);
        if (periods.isEmpty()) {
            throw new ServiceException("请先录入分节比分");
        }
        int homeTotal = 0;
        int awayTotal = 0;
        int otCount = 0;
        for (MatchPeriodScoreVo p : periods) {
            homeTotal += p.getHomeScore() != null ? p.getHomeScore() : 0;
            awayTotal += p.getAwayScore() != null ? p.getAwayScore() : 0;
            if (MatchPeriodTypeEnum.OVERTIME.getCode().equalsIgnoreCase(p.getPeriodType())) {
                otCount++;
            }
        }
        if (homeTotal == awayTotal && otCount == 0) {
            throw new ServiceException("常规时间平局须通过加时赛分出胜负");
        }
        applyFinalScore(match, homeTotal, awayTotal, otCount, null, MatchStatusEnum.FINISHED.getCode(),
                MatchDataStatusEnum.SUBMITTED.getCode());
        match.setMatchStatus(MatchStatusEnum.FINISHED.getCode());
        match.setDataStatus(MatchDataStatusEnum.SUBMITTED.getCode());
        validationService.writeStatusLog(match, MatchStatusEnum.RUNNING.getCode(), MatchChangeTypeEnum.SCORE_SUBMIT, "提交比分");
        validationService.writeStatusLog(match, MatchStatusEnum.RUNNING.getCode(), MatchChangeTypeEnum.FINISH, "比赛结束");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmScore(Long matchId) {
        BasketMatch match = validationService.requireMatch(matchId);
        if (match.getDataStatus() == null || match.getDataStatus() != MatchDataStatusEnum.SUBMITTED.getCode()) {
            throw new ServiceException("仅已提交比分可确认");
        }
        validationService.writeStatusLog(match, match.getMatchStatus(), MatchChangeTypeEnum.SCORE_CONFIRM, "确认比分");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reopenScore(Long matchId, MatchReopenReq req) {
        if (StringUtils.isEmpty(req.getReason())) {
            throw new ServiceException("重开原因不能为空");
        }
        BasketMatch match = validationService.requireMatch(matchId);
        if (match.getDataStatus() == null || match.getDataStatus() != MatchDataStatusEnum.SUBMITTED.getCode()) {
            throw new ServiceException("仅已提交比分可重开");
        }
        if (matchMapper.updateDataStatus(matchId, Collections.singletonList(MatchDataStatusEnum.SUBMITTED.getCode()),
                MatchDataStatusEnum.REOPENED.getCode()) != 1) {
            throw new ServiceException("重开失败");
        }
        if (matchMapper.updateStatusIfMatch(matchId, Collections.singletonList(MatchStatusEnum.FINISHED.getCode()),
                MatchStatusEnum.RUNNING.getCode()) != 1) {
            throw new ServiceException("比赛状态回退失败");
        }
        match.setDataStatus(MatchDataStatusEnum.REOPENED.getCode());
        match.setMatchStatus(MatchStatusEnum.RUNNING.getCode());
        validationService.writeStatusLog(match, MatchStatusEnum.FINISHED.getCode(), MatchChangeTypeEnum.REOPEN, req.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forfeit(Long matchId, MatchForfeitReq req) {
        BasketMatch match = validationService.requireMatch(matchId);
        assertScoreEditable(match);
        if (req.getWalkoverTeamId() == null) {
            throw new ServiceException("判胜球队不能为空");
        }
        if (!req.getWalkoverTeamId().equals(match.getHomeTeamId()) && !req.getWalkoverTeamId().equals(match.getAwayTeamId())) {
            throw new ServiceException("判胜球队必须是参赛球队");
        }
        Long loserId = req.getWalkoverTeamId().equals(match.getHomeTeamId()) ? match.getAwayTeamId() : match.getHomeTeamId();
        int homeScore = req.getWalkoverTeamId().equals(match.getHomeTeamId())
                ? BasketMatchConstants.WALKOVER_WIN_SCORE : BasketMatchConstants.WALKOVER_LOSS_SCORE;
        int awayScore = req.getWalkoverTeamId().equals(match.getAwayTeamId())
                ? BasketMatchConstants.WALKOVER_WIN_SCORE : BasketMatchConstants.WALKOVER_LOSS_SCORE;
        applyFinalScore(match, homeScore, awayScore, 0, req.getWalkoverTeamId(), MatchStatusEnum.FINISHED.getCode(),
                MatchDataStatusEnum.SUBMITTED.getCode());
        matchTeamMapper.updateScoreAndResult(matchId, req.getWalkoverTeamId(), Math.max(homeScore, awayScore),
                MatchResultTypeEnum.FORFEIT_WIN.getCode(), 1);
        matchTeamMapper.updateScoreAndResult(matchId, loserId, Math.min(homeScore, awayScore),
                MatchResultTypeEnum.FORFEIT_LOSS.getCode(), 0);
        match.setMatchStatus(MatchStatusEnum.FINISHED.getCode());
        match.setDataStatus(MatchDataStatusEnum.SUBMITTED.getCode());
        validationService.writeStatusLog(match, match.getMatchStatus(), MatchChangeTypeEnum.FINISH,
                StringUtils.isNotEmpty(req.getReason()) ? req.getReason() : "判负结束（默认比分20-0）");
    }

    private void assertScoreEditable(BasketMatch match) {
        MatchStatusEnum status = MatchStatusEnum.fromCode(match.getMatchStatus());
        MatchDataStatusEnum dataStatus = MatchDataStatusEnum.fromCode(match.getDataStatus());
        if (!status.canEnterScore() || !dataStatus.canEdit()) {
            throw new ServiceException("当前状态不可录入比分");
        }
    }

    private void applyFinalScore(BasketMatch match, int homeScore, int awayScore, int otCount, Long walkoverTeamId,
                                 int matchStatus, int dataStatus) {
        Long winnerId = null;
        String homeResult = MatchResultTypeEnum.LOSS.getCode();
        String awayResult = MatchResultTypeEnum.LOSS.getCode();
        int homeWinner = 0;
        int awayWinner = 0;
        if (homeScore > awayScore) {
            winnerId = match.getHomeTeamId();
            homeResult = walkoverTeamId != null ? MatchResultTypeEnum.FORFEIT_WIN.getCode() : MatchResultTypeEnum.WIN.getCode();
            awayResult = walkoverTeamId != null ? MatchResultTypeEnum.FORFEIT_LOSS.getCode() : MatchResultTypeEnum.LOSS.getCode();
            homeWinner = 1;
        } else if (awayScore > homeScore) {
            winnerId = match.getAwayTeamId();
            awayResult = walkoverTeamId != null ? MatchResultTypeEnum.FORFEIT_WIN.getCode() : MatchResultTypeEnum.WIN.getCode();
            homeResult = walkoverTeamId != null ? MatchResultTypeEnum.FORFEIT_LOSS.getCode() : MatchResultTypeEnum.LOSS.getCode();
            awayWinner = 1;
        }
        Date now = new Date();
        matchMapper.updateScores(match.getId(), homeScore, awayScore, winnerId, otCount, walkoverTeamId,
                matchStatus, dataStatus, now);
        matchTeamMapper.updateScoreAndResult(match.getId(), match.getHomeTeamId(), homeScore, homeResult, homeWinner);
        matchTeamMapper.updateScoreAndResult(match.getId(), match.getAwayTeamId(), awayScore, awayResult, awayWinner);
    }
}
