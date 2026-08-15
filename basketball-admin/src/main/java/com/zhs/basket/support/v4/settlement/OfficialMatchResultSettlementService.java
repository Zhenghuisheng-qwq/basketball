package com.zhs.basket.support.v4.settlement;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketMatchSubmission;
import com.zhs.basket.domain.v4.BasketMatchSubmissionPlayerStat;
import com.zhs.basket.domain.v4.BasketMatchSubmissionTeamStat;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.domain.v4.BasketV4MatchPlayerStat;
import com.zhs.basket.domain.v4.BasketV4MatchTeamStat;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.basket.enums.v4.MatchStatusEnum;
import com.zhs.basket.enums.v4.OfficialResultSource;
import com.zhs.basket.mapper.v4.BasketDivisionStageMapper;
import com.zhs.basket.mapper.v4.BasketMatchSubmissionMapper;
import com.zhs.basket.mapper.v4.BasketMatchSubmissionPlayerStatMapper;
import com.zhs.basket.mapper.v4.BasketMatchSubmissionTeamStatMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchPlayerStatMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchTeamStatMapper;
import com.zhs.basket.service.v4.IBasketMatchSeriesService;
import com.zhs.basket.service.v4.IBasketV4StatsSettlementService;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 统一正式比赛结果结算：Match → 本场 Team Stat → Series → Standing / 派生统计。
 */
@Service
public class OfficialMatchResultSettlementService {

    private static final Integer NOT_DELETED = 0;

    @Autowired
    private BasketV4MatchMapper matchMapper;
    @Autowired
    private BasketMatchSubmissionMapper submissionMapper;
    @Autowired
    private BasketMatchSubmissionTeamStatMapper submissionTeamStatMapper;
    @Autowired
    private BasketMatchSubmissionPlayerStatMapper submissionPlayerStatMapper;
    @Autowired
    private BasketV4MatchTeamStatMapper matchTeamStatMapper;
    @Autowired
    private BasketV4MatchPlayerStatMapper matchPlayerStatMapper;
    @Autowired
    private BasketDivisionStageMapper stageMapper;
    @Autowired
    private IBasketMatchSeriesService matchSeriesService;
    @Autowired
    @Lazy
    private IBasketV4StatsSettlementService statsSettlementService;

    @Transactional(rollbackFor = Exception.class)
    public void settleFromSubmission(Long submissionId) {
        BasketMatchSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new ServiceException("提交不存在");
        }
        if (submission.getHomeScore() == null || submission.getAwayScore() == null) {
            throw new ServiceException("提交缺少总比分");
        }
        BasketV4Match locked = requireMatchForUpdate(submission.getMatchId());
        rejectDrawForElimination(locked, submission.getHomeScore(), submission.getAwayScore());
        Long winnerId = resolveWinner(locked.getHomeEventTeamId(), locked.getAwayEventTeamId(),
                submission.getHomeScore(), submission.getAwayScore());
        OfficialMatchResultCommand command = new OfficialMatchResultCommand();
        command.setMatchId(locked.getId());
        command.setHomeScore(submission.getHomeScore());
        command.setAwayScore(submission.getAwayScore());
        command.setWinnerEventTeamId(winnerId);
        command.setLoserEventTeamId(resolveLoser(locked.getHomeEventTeamId(), locked.getAwayEventTeamId(), winnerId));
        command.setMatchStatus(MatchStatusEnum.FINISHED.getCode());
        command.setSource(OfficialResultSource.SUBMISSION);
        command.setSourceSubmissionId(submission.getId());
        applyLocked(locked, command);
        applySubmissionStats(submission, locked, new Date());
        rebuildDownstream(locked);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean settle(OfficialMatchResultCommand command) {
        if (command == null || command.getMatchId() == null) {
            throw new ServiceException("matchId 不能为空");
        }
        BasketV4Match locked = requireMatchForUpdate(command.getMatchId());
        if (isSameOfficialOutcome(locked, command)) {
            return false;
        }
        if (OfficialResultSource.ADMIN_DIRECT.equals(command.getSource())
                && isOfficialSettled(locked.getMatchStatus())) {
            throw new ServiceException("比赛已有正式结果，请使用修正比赛结果");
        }
        if (OfficialResultSource.ADMIN_CORRECTION.equals(command.getSource())
                && !MatchStatusEnum.FINISHED.getCode().equals(locked.getMatchStatus())) {
            throw new ServiceException("仅已结算的正式比分可修正");
        }
        if (OfficialResultSource.FORFEIT.equals(command.getSource())
                && isOfficialSettled(locked.getMatchStatus())) {
            throw new ServiceException("比赛已有正式结果，无法重复判负");
        }
        if (MatchStatusEnum.FINISHED.getCode().equals(command.getMatchStatus())
                && command.getHomeScore() != null && command.getAwayScore() != null) {
            rejectDrawForElimination(locked, command.getHomeScore(), command.getAwayScore());
        }
        applyLocked(locked, command);
        Date now = new Date();
        if (command.isClearOfficialPlayerStats()) {
            matchPlayerStatMapper.softDeleteByMatchId(locked.getId());
        }
        if (OfficialResultSource.FORFEIT.equals(command.getSource())) {
            upsertForfeitTeamStats(locked, now);
        } else if (command.isScoreOnlyTeamStats()) {
            upsertScoreOnlyTeamStats(locked, now, command.getSourceSubmissionId());
        }
        rebuildDownstream(locked);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public void unsetOfficialResult(Long matchId) {
        BasketV4Match locked = requireMatchForUpdate(matchId);
        if (!MatchStatusEnum.FORFEITED.getCode().equals(locked.getMatchStatus())) {
            throw new ServiceException("仅已判负的比赛可以撤销判负");
        }
        if (matchMapper.clearOfficialResult(locked.getId(), MatchStatusEnum.SCHEDULED.getCode()) != 1) {
            throw new ServiceException("撤销比赛判负失败");
        }
        locked.setHomeScore(null);
        locked.setAwayScore(null);
        locked.setWinnerEventTeamId(null);
        locked.setLoserEventTeamId(null);
        locked.setMatchStatus(MatchStatusEnum.SCHEDULED.getCode());
        matchPlayerStatMapper.softDeleteByMatchId(locked.getId());
        matchTeamStatMapper.softDeleteByMatchId(locked.getId());
        rebuildDownstream(locked);
    }

    private void applyLocked(BasketV4Match match, OfficialMatchResultCommand command) {
        Date now = new Date();
        match.setHomeScore(command.getHomeScore());
        match.setAwayScore(command.getAwayScore());
        match.setWinnerEventTeamId(command.getWinnerEventTeamId());
        match.setLoserEventTeamId(command.getLoserEventTeamId());
        match.setMatchStatus(command.getMatchStatus());
        if (command.getRemark() != null) {
            match.setRemark(command.getRemark());
        }
        match.setUpdateTime(now);
        if (matchMapper.updateById(match) != 1) {
            throw new ServiceException("更新官方比赛结果失败");
        }
    }

    private void applySubmissionStats(BasketMatchSubmission submission, BasketV4Match match, Date now) {
        BasketMatchSubmissionTeamStat subTeamStat = submissionTeamStatMapper.selectBySubmissionId(submission.getId());
        upsertSubmitTeamMatchStat(submission, match, subTeamStat, now);
        upsertOpponentMinimalTeamStat(submission, match, now);
        List<BasketMatchSubmissionPlayerStat> subPlayerStats = submissionPlayerStatMapper.selectBySubmissionId(submission.getId());
        for (BasketMatchSubmissionPlayerStat ps : subPlayerStats) {
            upsertMatchPlayerStat(submission, match, ps, now);
        }
    }

    private void upsertScoreOnlyTeamStats(BasketV4Match match, Date now, Long sourceSubmissionId) {
        upsertOutcomeTeamStat(match, match.getHomeEventTeamId(), match.getAwayEventTeamId(),
                match.getHomeScore(), match.getAwayScore(), now, sourceSubmissionId, true);
        upsertOutcomeTeamStat(match, match.getAwayEventTeamId(), match.getHomeEventTeamId(),
                match.getAwayScore(), match.getHomeScore(), now, sourceSubmissionId, true);
    }

    private void upsertForfeitTeamStats(BasketV4Match match, Date now) {
        upsertOutcomeTeamStat(match, match.getHomeEventTeamId(), match.getAwayEventTeamId(),
                null, null, now, null, false);
        upsertOutcomeTeamStat(match, match.getAwayEventTeamId(), match.getHomeEventTeamId(),
                null, null, now, null, false);
    }

    private void upsertOutcomeTeamStat(BasketV4Match match, Long eventTeamId, Long opponentId,
                                       Integer teamScore, Integer oppScore, Date now,
                                       Long sourceSubmissionId, boolean resetBoxScore) {
        if (eventTeamId == null) {
            return;
        }
        BasketV4MatchTeamStat existing = matchTeamStatMapper.selectByMatchAndEventTeam(match.getId(), eventTeamId);
        BasketV4MatchTeamStat row = existing != null ? existing : new BasketV4MatchTeamStat();
        row.setMatchId(match.getId());
        row.setSeasonId(match.getSeasonId());
        row.setDivisionId(match.getDivisionId());
        row.setEventTeamId(eventTeamId);
        row.setOpponentEventTeamId(opponentId);
        row.setIsWinner(eventTeamId.equals(match.getWinnerEventTeamId()) ? 1 : 0);
        row.setScore(nvl(teamScore));
        row.setPointsFor(nvl(teamScore));
        row.setPointsAgainst(nvl(oppScore));
        if (resetBoxScore || existing == null) {
            applyTeamStatDefaults(row);
        }
        row.setSourceSubmissionId(sourceSubmissionId);
        row.setUpdateTime(now);
        if (existing == null) {
            row.setCreateTime(now);
            row.setIsDelete(NOT_DELETED);
            matchTeamStatMapper.insert(row);
        } else {
            matchTeamStatMapper.updateById(row);
        }
    }

    private void upsertSubmitTeamMatchStat(BasketMatchSubmission submission, BasketV4Match match,
                                           BasketMatchSubmissionTeamStat subTeamStat, Date now) {
        boolean isHome = submission.getSubmitEventTeamId().equals(match.getHomeEventTeamId());
        int teamScore = isHome ? submission.getHomeScore() : submission.getAwayScore();
        int oppScore = isHome ? submission.getAwayScore() : submission.getHomeScore();
        BasketV4MatchTeamStat existing = matchTeamStatMapper.selectByMatchAndEventTeam(match.getId(), submission.getSubmitEventTeamId());
        BasketV4MatchTeamStat row = existing != null ? existing : new BasketV4MatchTeamStat();
        row.setMatchId(match.getId());
        row.setSeasonId(match.getSeasonId());
        row.setDivisionId(match.getDivisionId());
        row.setEventTeamId(submission.getSubmitEventTeamId());
        row.setOpponentEventTeamId(submission.getOpponentEventTeamId());
        row.setIsWinner(teamScore > oppScore ? 1 : 0);
        row.setScore(teamScore);
        row.setPointsFor(teamScore);
        row.setPointsAgainst(oppScore);
        if (subTeamStat != null) {
            row.setFieldGoalMade(nvl(subTeamStat.getFieldGoalMade()));
            row.setFieldGoalAttempt(nvl(subTeamStat.getFieldGoalAttempt()));
            row.setThreePointMade(nvl(subTeamStat.getThreePointMade()));
            row.setThreePointAttempt(nvl(subTeamStat.getThreePointAttempt()));
            row.setFreeThrowMade(nvl(subTeamStat.getFreeThrowMade()));
            row.setFreeThrowAttempt(nvl(subTeamStat.getFreeThrowAttempt()));
            row.setRebound(nvl(subTeamStat.getRebound()));
            row.setAssist(nvl(subTeamStat.getAssist()));
            row.setSteal(nvl(subTeamStat.getSteal()));
            row.setBlock(nvl(subTeamStat.getBlock()));
            row.setTurnover(nvl(subTeamStat.getTurnover()));
            row.setFoul(nvl(subTeamStat.getFoul()));
        }
        applyTeamStatDefaults(row);
        row.setSourceSubmissionId(submission.getId());
        row.setUpdateTime(now);
        if (existing == null) {
            row.setCreateTime(now);
            row.setIsDelete(NOT_DELETED);
            matchTeamStatMapper.insert(row);
        } else {
            matchTeamStatMapper.updateById(row);
        }
    }

    private void upsertOpponentMinimalTeamStat(BasketMatchSubmission submission, BasketV4Match match, Date now) {
        Long oppTeamId = submission.getOpponentEventTeamId();
        if (oppTeamId == null) {
            return;
        }
        boolean submitIsHome = submission.getSubmitEventTeamId().equals(match.getHomeEventTeamId());
        int oppScore = submitIsHome ? submission.getAwayScore() : submission.getHomeScore();
        int teamScore = submitIsHome ? submission.getHomeScore() : submission.getAwayScore();
        BasketV4MatchTeamStat existing = matchTeamStatMapper.selectByMatchAndEventTeam(match.getId(), oppTeamId);
        if (existing != null && existing.getSourceSubmissionId() != null
                && !existing.getSourceSubmissionId().equals(submission.getId())) {
            return;
        }
        BasketV4MatchTeamStat row = existing != null ? existing : new BasketV4MatchTeamStat();
        row.setMatchId(match.getId());
        row.setSeasonId(match.getSeasonId());
        row.setDivisionId(match.getDivisionId());
        row.setEventTeamId(oppTeamId);
        row.setOpponentEventTeamId(submission.getSubmitEventTeamId());
        row.setIsWinner(oppScore > teamScore ? 1 : 0);
        row.setScore(oppScore);
        row.setPointsFor(oppScore);
        row.setPointsAgainst(teamScore);
        row.setSourceSubmissionId(submission.getId());
        applyTeamStatDefaults(row);
        row.setUpdateTime(now);
        if (existing == null) {
            row.setCreateTime(now);
            row.setIsDelete(NOT_DELETED);
            matchTeamStatMapper.insert(row);
        } else {
            matchTeamStatMapper.updateById(row);
        }
    }

    private void upsertMatchPlayerStat(BasketMatchSubmission submission, BasketV4Match match,
                                       BasketMatchSubmissionPlayerStat ps, Date now) {
        BasketV4MatchPlayerStat existing = matchPlayerStatMapper.selectByMatchAndEventPlayer(match.getId(), ps.getEventPlayerId());
        BasketV4MatchPlayerStat row = existing != null ? existing : new BasketV4MatchPlayerStat();
        row.setMatchId(match.getId());
        row.setSeasonId(match.getSeasonId());
        row.setDivisionId(match.getDivisionId());
        row.setEventTeamId(ps.getEventTeamId());
        row.setEventPlayerId(ps.getEventPlayerId());
        row.setPoints(nvl(ps.getPoints()));
        row.setScore(nvl(ps.getPoints()));
        row.setRebound(nvl(ps.getRebound()));
        row.setAssist(nvl(ps.getAssist()));
        row.setSteal(nvl(ps.getSteal()));
        row.setBlock(nvl(ps.getBlock()));
        row.setTurnover(nvl(ps.getTurnover()));
        row.setFoul(nvl(ps.getFoul()));
        row.setMinutes(ps.getMinutes());
        row.setSourceSubmissionId(submission.getId());
        row.setUpdateTime(now);
        if (existing == null) {
            row.setCreateTime(now);
            row.setIsDelete(NOT_DELETED);
            matchPlayerStatMapper.insert(row);
        } else {
            matchPlayerStatMapper.updateById(row);
        }
    }

    private void rebuildDownstream(BasketV4Match match) {
        if (match.getSeriesId() != null) {
            matchSeriesService.rebuildSeriesResult(match.getSeriesId());
        }
        if (match.getStageId() != null) {
            statsSettlementService.rebuildStageStats(match.getStageId());
        } else if (match.getDivisionId() != null) {
            statsSettlementService.rebuildDivisionStats(match.getDivisionId());
        }
    }

    private BasketV4Match requireMatchForUpdate(Long matchId) {
        BasketV4Match match = matchMapper.selectByIdForUpdate(matchId);
        if (match == null) {
            throw new ServiceException("比赛不存在");
        }
        return match;
    }

    private void rejectDrawForElimination(BasketV4Match match, int homeScore, int awayScore) {
        if (homeScore == awayScore && match.getStageId() != null) {
            BasketDivisionStage stage = stageMapper.selectById(match.getStageId());
            if (stage != null
                    && CompetitionFormatEnum.SINGLE_ELIMINATION.getCode().equals(stage.getCompetitionFormat())) {
                throw new ServiceException("淘汰赛不允许平局");
            }
        }
    }

    private boolean isSameOfficialOutcome(BasketV4Match match, OfficialMatchResultCommand command) {
        return sameNumber(match.getHomeScore(), command.getHomeScore())
                && sameNumber(match.getAwayScore(), command.getAwayScore())
                && sameId(match.getWinnerEventTeamId(), command.getWinnerEventTeamId())
                && sameText(match.getMatchStatus(), command.getMatchStatus());
    }

    private boolean isOfficialSettled(String status) {
        return MatchStatusEnum.FINISHED.getCode().equals(status)
                || MatchStatusEnum.FORFEITED.getCode().equals(status);
    }

    public static Long resolveWinner(Long homeTeamId, Long awayTeamId, int homeScore, int awayScore) {
        if (homeScore > awayScore) {
            return homeTeamId;
        }
        if (awayScore > homeScore) {
            return awayTeamId;
        }
        return null;
    }

    public static Long resolveLoser(Long homeTeamId, Long awayTeamId, Long winnerId) {
        if (winnerId == null) {
            return null;
        }
        if (winnerId.equals(homeTeamId)) {
            return awayTeamId;
        }
        if (winnerId.equals(awayTeamId)) {
            return homeTeamId;
        }
        return null;
    }

    private void applyTeamStatDefaults(BasketV4MatchTeamStat row) {
        if (row.getFieldGoalMade() == null) {
            row.setFieldGoalMade(0);
        }
        if (row.getFieldGoalAttempt() == null) {
            row.setFieldGoalAttempt(0);
        }
        if (row.getThreePointMade() == null) {
            row.setThreePointMade(0);
        }
        if (row.getThreePointAttempt() == null) {
            row.setThreePointAttempt(0);
        }
        if (row.getFreeThrowMade() == null) {
            row.setFreeThrowMade(0);
        }
        if (row.getFreeThrowAttempt() == null) {
            row.setFreeThrowAttempt(0);
        }
        if (row.getRebound() == null) {
            row.setRebound(0);
        }
        if (row.getAssist() == null) {
            row.setAssist(0);
        }
        if (row.getSteal() == null) {
            row.setSteal(0);
        }
        if (row.getBlock() == null) {
            row.setBlock(0);
        }
        if (row.getTurnover() == null) {
            row.setTurnover(0);
        }
        if (row.getFoul() == null) {
            row.setFoul(0);
        }
    }

    private int nvl(Integer v) {
        return v == null ? 0 : v;
    }

    private boolean sameNumber(Integer a, Integer b) {
        return a == null ? b == null : a.equals(b);
    }

    private boolean sameId(Long a, Long b) {
        return a == null ? b == null : a.equals(b);
    }

    private boolean sameText(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
