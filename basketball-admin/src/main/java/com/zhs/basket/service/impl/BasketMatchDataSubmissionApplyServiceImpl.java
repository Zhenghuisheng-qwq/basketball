package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketMatch;
import com.zhs.basket.domain.BasketMatchDataSubmission;
import com.zhs.basket.domain.BasketMatchDataSubmissionPeriod;
import com.zhs.basket.domain.BasketMatchDataSubmissionPlayerStat;
import com.zhs.basket.domain.BasketMatchDataSubmissionTeamStat;
import com.zhs.basket.domain.BasketMatchOfficialResult;
import com.zhs.basket.domain.BasketMatchPeriodScore;
import com.zhs.basket.domain.BasketMatchPlayerStat;
import com.zhs.basket.domain.BasketMatchTeamStat;
import com.zhs.basket.domain.BasketStatChangeRecord;
import com.zhs.basket.enums.MatchChangeTypeEnum;
import com.zhs.basket.enums.MatchDataStatusEnum;
import com.zhs.basket.enums.MatchPeriodTypeEnum;
import com.zhs.basket.enums.MatchResultTypeEnum;
import com.zhs.basket.enums.MatchStatusEnum;
import com.zhs.basket.mapper.BasketMatchDataSubmissionMapper;
import com.zhs.basket.mapper.BasketMatchDataSubmissionPeriodMapper;
import com.zhs.basket.mapper.BasketMatchDataSubmissionPlayerStatMapper;
import com.zhs.basket.mapper.BasketMatchDataSubmissionTeamStatMapper;
import com.zhs.basket.mapper.BasketMatchMapper;
import com.zhs.basket.mapper.BasketMatchPeriodScoreMapper;
import com.zhs.basket.mapper.BasketMatchPlayerStatMapper;
import com.zhs.basket.mapper.BasketMatchTeamMapper;
import com.zhs.basket.mapper.BasketMatchTeamStatMapper;
import com.zhs.basket.mapper.BasketStatChangeRecordMapper;
import com.zhs.basket.service.IBasketDataSettlementService;
import com.zhs.basket.service.IBasketMatchDataSubmissionApplyService;
import com.zhs.basket.service.IBasketMatchOfficialResultService;
import com.zhs.basket.service.IBasketMatchStatSyncService;
import com.zhs.basket.service.IBasketMatchValidationService;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
public class BasketMatchDataSubmissionApplyServiceImpl implements IBasketMatchDataSubmissionApplyService {

    private static final Integer NOT_DELETED = 0;
    private static final int PERIOD_STATUS_CONFIRMED = 1;
    private static final int APPROVAL_NONE = 0;
    private static final String BIZ_TYPE_MATCH = "MATCH";

    @Autowired private BasketMatchDataSubmissionMapper submissionMapper;
    @Autowired private BasketMatchDataSubmissionPeriodMapper submissionPeriodMapper;
    @Autowired private BasketMatchDataSubmissionTeamStatMapper submissionTeamStatMapper;
    @Autowired private BasketMatchDataSubmissionPlayerStatMapper submissionPlayerStatMapper;
    @Autowired private BasketMatchMapper matchMapper;
    @Autowired private BasketMatchPeriodScoreMapper periodScoreMapper;
    @Autowired private BasketMatchTeamStatMapper teamStatMapper;
    @Autowired private BasketMatchPlayerStatMapper playerStatMapper;
    @Autowired private BasketMatchTeamMapper matchTeamMapper;
    @Autowired private BasketStatChangeRecordMapper statChangeRecordMapper;
    @Autowired private IBasketMatchValidationService validationService;
    @Autowired private IBasketMatchOfficialResultService officialResultService;
    @Autowired private IBasketMatchStatSyncService statSyncService;
    @Lazy
    @Autowired private IBasketDataSettlementService dataSettlementService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyApprovedSubmissionToOfficialData(Long submissionId, Long operatorId) {
        BasketMatchDataSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new ServiceException("提交记录不存在");
        }
        BasketMatch match = matchMapper.selectById(submission.getMatchId());
        if (match == null) {
            throw new ServiceException("比赛不存在");
        }
        List<BasketMatchDataSubmissionPeriod> periods = submissionPeriodMapper.selectBySubmissionId(submissionId);
        List<BasketMatchDataSubmissionTeamStat> teamStats = submissionTeamStatMapper.selectBySubmissionId(submissionId);
        List<BasketMatchDataSubmissionPlayerStat> playerStats = submissionPlayerStatMapper.selectBySubmissionId(submissionId);
        if (CollectionUtils.isEmpty(periods) || CollectionUtils.isEmpty(teamStats) || CollectionUtils.isEmpty(playerStats)) {
            throw new ServiceException("提交数据不完整，无法同步正式表");
        }

        int homeScore = nvl(submission.getHomeScore());
        int awayScore = nvl(submission.getAwayScore());
        int otCount = 0;
        for (BasketMatchDataSubmissionPeriod period : periods) {
            if (MatchPeriodTypeEnum.OVERTIME.getCode().equalsIgnoreCase(period.getPeriodType())) {
                otCount++;
            }
        }
        Long winnerId = resolveWinnerId(submission, homeScore, awayScore);
        Date now = new Date();

        syncPeriodScores(submission.getMatchId(), periods, now);
        syncTeamStats(submission, teamStats, operatorId, now);
        syncPlayerStats(submission, playerStats, operatorId, now);
        syncMatchScore(match, homeScore, awayScore, winnerId, otCount, now);
        syncMatchTeam(match.getId(), submission.getHomeTeamId(), submission.getAwayTeamId(), homeScore, awayScore);

        int beforeStatus = match.getDataStatus() != null ? match.getDataStatus() : MatchDataStatusEnum.SUBMITTED.getCode();
        matchMapper.updateScoreConfirmed(match.getId(),
                Collections.singletonList(MatchDataStatusEnum.CONFIRMED.getCode()), 1);
        int dataVersion = match.getDataVersion() != null ? match.getDataVersion() : 1;
        matchMapper.updateResultMeta(match.getId(), 1, dataVersion, 1);

        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);
        match.setWinnerTeamId(winnerId);
        match.setDataStatus(MatchDataStatusEnum.CONFIRMED.getCode());
        validationService.writeStatusLog(match, match.getMatchStatus(), MatchChangeTypeEnum.RESULT_CONFIRM,
                "审核通过队长提交，同步正式比赛数据");
        writeStatSummary(match, beforeStatus, MatchDataStatusEnum.CONFIRMED.getCode(), "审核通过队长提交");

        BasketMatchOfficialResult officialResult = officialResultService.publishFromMatch(match, operatorId);
        if (statSyncService.tryRecordSync(officialResult, "SUBMISSION_APPROVE")) {
            dataSettlementService.rebuildMatch(match.getId());
        }
    }

    private void syncPeriodScores(Long matchId, List<BasketMatchDataSubmissionPeriod> periods, Date now) {
        List<BasketMatchPeriodScore> list = new ArrayList<>();
        for (BasketMatchDataSubmissionPeriod period : periods) {
            BasketMatchPeriodScore row = new BasketMatchPeriodScore();
            row.setMatchId(matchId);
            row.setPeriodNo(period.getPeriodNo());
            row.setPeriodType(period.getPeriodType());
            row.setHomeScore(nvl(period.getHomeScore()));
            row.setAwayScore(nvl(period.getAwayScore()));
            row.setPeriodStatus(PERIOD_STATUS_CONFIRMED);
            row.setCreateTime(now);
            row.setUpdateTime(now);
            row.setIsDelete(NOT_DELETED);
            list.add(row);
        }
        periodScoreMapper.softDeleteByMatchId(matchId);
        periodScoreMapper.batchInsert(list);
    }

    private void syncTeamStats(BasketMatchDataSubmission submission, List<BasketMatchDataSubmissionTeamStat> teamStats,
            Long operatorId, Date now) {
        List<BasketMatchTeamStat> list = new ArrayList<>();
        for (BasketMatchDataSubmissionTeamStat stat : teamStats) {
            BasketMatchTeamStat row = new BasketMatchTeamStat();
            row.setMatchId(submission.getMatchId());
            row.setSeasonId(submission.getSeasonId());
            row.setStageId(submission.getStageId());
            row.setTeamId(stat.getTeamId());
            row.setOpponentTeamId(stat.getOpponentTeamId());
            row.setPoints(nvl(stat.getPoints()));
            row.setRebounds(nvl(stat.getRebounds()));
            row.setOffensiveRebounds(nvl(stat.getOffensiveRebounds()));
            row.setDefensiveRebounds(nvl(stat.getDefensiveRebounds()));
            row.setAssists(nvl(stat.getAssists()));
            row.setSteals(nvl(stat.getSteals()));
            row.setBlocks(nvl(stat.getBlocks()));
            row.setTurnovers(nvl(stat.getTurnovers()));
            row.setFouls(nvl(stat.getFouls()));
            row.setFieldGoalsMade(nvl(stat.getFieldGoalsMade()));
            row.setFieldGoalsAttempted(nvl(stat.getFieldGoalsAttempted()));
            row.setFieldGoalPct(stat.getFieldGoalPct());
            row.setThreePointsMade(nvl(stat.getThreePointsMade()));
            row.setThreePointsAttempted(nvl(stat.getThreePointsAttempted()));
            row.setThreePointPct(stat.getThreePointPct());
            row.setFreeThrowsMade(nvl(stat.getFreeThrowsMade()));
            row.setFreeThrowsAttempted(nvl(stat.getFreeThrowsAttempted()));
            row.setFreeThrowPct(stat.getFreeThrowPct());
            row.setPossessions(stat.getPossessions());
            row.setEfficiency(stat.getEfficiency());
            row.setDataStatus(MatchDataStatusEnum.CONFIRMED.getCode());
            row.setConfirmedBy(operatorId);
            row.setConfirmedTime(now);
            row.setCreateTime(now);
            row.setUpdateTime(now);
            row.setIsDelete(NOT_DELETED);
            list.add(row);
        }
        teamStatMapper.softDeleteByMatchId(submission.getMatchId());
        teamStatMapper.batchInsert(list);
    }

    private void syncPlayerStats(BasketMatchDataSubmission submission, List<BasketMatchDataSubmissionPlayerStat> playerStats,
            Long operatorId, Date now) {
        List<BasketMatchPlayerStat> list = new ArrayList<>();
        for (BasketMatchDataSubmissionPlayerStat stat : playerStats) {
            BasketMatchPlayerStat row = new BasketMatchPlayerStat();
            row.setMatchId(submission.getMatchId());
            row.setSeasonId(submission.getSeasonId());
            row.setStageId(submission.getStageId());
            row.setTeamId(stat.getTeamId());
            row.setPlayerId(stat.getPlayerId());
            row.setRosterId(stat.getRosterId());
            row.setStartedFlag(stat.getStartedFlag() != null ? stat.getStartedFlag() : 0);
            row.setMinutesSeconds(nvl(stat.getMinutesSeconds()));
            row.setPoints(nvl(stat.getPoints()));
            row.setRebounds(nvl(stat.getRebounds()));
            row.setOffensiveRebounds(nvl(stat.getOffensiveRebounds()));
            row.setDefensiveRebounds(nvl(stat.getDefensiveRebounds()));
            row.setAssists(nvl(stat.getAssists()));
            row.setSteals(nvl(stat.getSteals()));
            row.setBlocks(nvl(stat.getBlocks()));
            row.setTurnovers(nvl(stat.getTurnovers()));
            row.setFouls(nvl(stat.getFouls()));
            row.setFieldGoalsMade(nvl(stat.getFieldGoalsMade()));
            row.setFieldGoalsAttempted(nvl(stat.getFieldGoalsAttempted()));
            row.setFieldGoalPct(stat.getFieldGoalPct());
            row.setThreePointsMade(nvl(stat.getThreePointsMade()));
            row.setThreePointsAttempted(nvl(stat.getThreePointsAttempted()));
            row.setThreePointPct(stat.getThreePointPct());
            row.setFreeThrowsMade(nvl(stat.getFreeThrowsMade()));
            row.setFreeThrowsAttempted(nvl(stat.getFreeThrowsAttempted()));
            row.setFreeThrowPct(stat.getFreeThrowPct());
            row.setPlusMinus(nvl(stat.getPlusMinus()));
            row.setEfficiency(stat.getEfficiency());
            row.setDoubleDouble(0);
            row.setTripleDouble(0);
            row.setDataStatus(MatchDataStatusEnum.CONFIRMED.getCode());
            row.setConfirmedBy(operatorId);
            row.setConfirmedTime(now);
            row.setCreateTime(now);
            row.setUpdateTime(now);
            row.setIsDelete(NOT_DELETED);
            list.add(row);
        }
        playerStatMapper.softDeleteByMatchId(submission.getMatchId());
        playerStatMapper.batchInsert(list);
    }

    private void syncMatchScore(BasketMatch match, int homeScore, int awayScore, Long winnerId, int otCount, Date now) {
        matchMapper.updateScores(match.getId(), homeScore, awayScore, winnerId, otCount, null,
                MatchStatusEnum.FINISHED.getCode(), MatchDataStatusEnum.CONFIRMED.getCode(), now);
    }

    private void syncMatchTeam(Long matchId, Long homeTeamId, Long awayTeamId, int homeScore, int awayScore) {
        String homeResult = MatchResultTypeEnum.LOSS.getCode();
        String awayResult = MatchResultTypeEnum.LOSS.getCode();
        int homeWinner = 0;
        int awayWinner = 0;
        if (homeScore > awayScore) {
            homeResult = MatchResultTypeEnum.WIN.getCode();
            homeWinner = 1;
        } else if (awayScore > homeScore) {
            awayResult = MatchResultTypeEnum.WIN.getCode();
            awayWinner = 1;
        }
        matchTeamMapper.updateScoreAndResult(matchId, homeTeamId, homeScore, homeResult, homeWinner);
        matchTeamMapper.updateScoreAndResult(matchId, awayTeamId, awayScore, awayResult, awayWinner);
    }

    private Long resolveWinnerId(BasketMatchDataSubmission submission, int homeScore, int awayScore) {
        if (submission.getWinnerTeamId() != null) {
            return submission.getWinnerTeamId();
        }
        if (homeScore > awayScore) {
            return submission.getHomeTeamId();
        }
        if (awayScore > homeScore) {
            return submission.getAwayTeamId();
        }
        return null;
    }

    private void writeStatSummary(BasketMatch match, int beforeStatus, int afterStatus, String reason) {
        Date now = new Date();
        BasketStatChangeRecord record = new BasketStatChangeRecord();
        record.setBizType(BIZ_TYPE_MATCH);
        record.setBizId(match.getId());
        record.setFieldName("data_status");
        record.setBeforeValue(String.valueOf(beforeStatus));
        record.setAfterValue(String.valueOf(afterStatus));
        record.setChangeReason(reason);
        record.setOperatorId(SecurityUtils.getUserId());
        record.setOperatorName(SecurityUtils.getUsername());
        record.setApprovalStatus(APPROVAL_NONE);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        record.setIsDelete(NOT_DELETED);
        statChangeRecordMapper.insert(record);
    }

    private int nvl(Integer value) {
        return value != null ? value : 0;
    }
}
