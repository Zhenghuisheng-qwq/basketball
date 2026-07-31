package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketMatch;
import com.zhs.basket.domain.BasketMatchDataRevision;
import com.zhs.basket.domain.BasketMatchDataSubmission;
import com.zhs.basket.domain.BasketMatchDataSubmissionMaterial;
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
import com.zhs.basket.mapper.BasketMatchDataRevisionMapper;
import com.zhs.basket.mapper.BasketMatchMapper;
import com.zhs.basket.mapper.BasketMatchPeriodScoreMapper;
import com.zhs.basket.mapper.BasketMatchPlayerStatMapper;
import com.zhs.basket.mapper.BasketMatchTeamMapper;
import com.zhs.basket.mapper.BasketMatchTeamStatMapper;
import com.zhs.basket.mapper.BasketStatChangeRecordMapper;
import com.zhs.basket.req.MatchOfficialDataCorrectionReq;
import com.zhs.basket.service.IBasketDataSettlementService;
import com.zhs.basket.service.IBasketMatchDataSubmissionValidationService;
import com.zhs.basket.service.IBasketMatchOfficialDataCorrectionService;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class BasketMatchOfficialDataCorrectionServiceImpl implements IBasketMatchOfficialDataCorrectionService {

    private static final Integer NOT_DELETED = 0;
    private static final int PERIOD_STATUS_CONFIRMED = 1;
    private static final int APPROVAL_NONE = 0;
    private static final String BIZ_TYPE_MATCH = "MATCH";
    private static final String MATERIAL_PLACEHOLDER = "RESULT_SCREENSHOT";

    @Autowired private BasketMatchMapper matchMapper;
    @Autowired private BasketMatchPeriodScoreMapper periodScoreMapper;
    @Autowired private BasketMatchTeamStatMapper teamStatMapper;
    @Autowired private BasketMatchPlayerStatMapper playerStatMapper;
    @Autowired private BasketMatchTeamMapper matchTeamMapper;
    @Autowired private BasketStatChangeRecordMapper statChangeRecordMapper;
    @Autowired private BasketMatchDataRevisionMapper revisionMapper;
    @Autowired private IBasketMatchValidationService validationService;
    @Autowired private IBasketMatchDataSubmissionValidationService submissionValidationService;
    @Autowired private IBasketMatchOfficialResultService officialResultService;
    @Autowired private IBasketMatchStatSyncService statSyncService;
    @Lazy @Autowired private IBasketDataSettlementService dataSettlementService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long applyCorrection(Long matchId, MatchOfficialDataCorrectionReq req, String source,
                                Long operatorId, Long disputeId, String reason) {
        if (req == null || matchId == null) {
            throw new ServiceException("修正参数不完整");
        }
        BasketMatch match = validationService.requireMatch(matchId);
        if (match.getDataStatus() == null || match.getDataStatus() != MatchDataStatusEnum.CONFIRMED.getCode()) {
            throw new ServiceException("仅已确认比赛可修正正式数据");
        }

        int homeScore = nvl(req.getHomeScore());
        int awayScore = nvl(req.getAwayScore());
        List<BasketMatchDataSubmissionPeriod> periods = toPeriods(matchId, req);
        List<BasketMatchDataSubmissionTeamStat> teamStats = toTeamStats(match, req);
        List<BasketMatchDataSubmissionPlayerStat> playerStats = toPlayerStats(match, req);
        BasketMatchDataSubmission pseudo = buildPseudoSubmission(match, homeScore, awayScore);
        submissionValidationService.validateCorrectionData(pseudo, periods, teamStats, playerStats);

        BasketMatchOfficialResult currentOfficial = officialResultService.getCurrent(matchId);
        int fromVersion = match.getDataVersion() != null ? match.getDataVersion() : 1;
        int toVersion = fromVersion + 1;
        Date now = new Date();
        int beforeHome = match.getHomeScore() != null ? match.getHomeScore() : 0;
        int beforeAway = match.getAwayScore() != null ? match.getAwayScore() : 0;

        int otCount = 0;
        for (BasketMatchDataSubmissionPeriod period : periods) {
            if (MatchPeriodTypeEnum.OVERTIME.getCode().equalsIgnoreCase(period.getPeriodType())) {
                otCount++;
            }
        }
        Long winnerId = resolveWinnerId(match, homeScore, awayScore);

        syncPeriodScores(matchId, periods, now);
        syncTeamStats(match, teamStats, operatorId, now);
        syncPlayerStats(match, playerStats, operatorId, now);
        syncMatchScore(match, homeScore, awayScore, winnerId, otCount, now);
        syncMatchTeam(match.getId(), match.getHomeTeamId(), match.getAwayTeamId(), homeScore, awayScore);

        matchMapper.updateResultMeta(match.getId(), 1, toVersion, 1);
        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);
        match.setWinnerTeamId(winnerId);
        match.setDataVersion(toVersion);
        match.setDataStatus(MatchDataStatusEnum.CONFIRMED.getCode());

        writeStatChange(match, "home_score", String.valueOf(beforeHome), String.valueOf(homeScore), reason);
        writeStatChange(match, "away_score", String.valueOf(beforeAway), String.valueOf(awayScore), reason);

        validationService.writeStatusLog(match, match.getMatchStatus(), MatchChangeTypeEnum.RESULT_CONFIRM,
                reason != null ? reason : "正式数据修正");

        Long revisionId = writeRevision(match, currentOfficial, source, disputeId, operatorId, reason, fromVersion, toVersion, now);
        BasketMatchOfficialResult officialResult = officialResultService.publishFromMatch(match, operatorId);
        String syncSource = source != null ? source : "OFFICIAL_CORRECTION";
        if (statSyncService.tryRecordSync(officialResult, syncSource)) {
            dataSettlementService.rebuildMatch(matchId);
        }
        return revisionId;
    }

    private BasketMatchDataSubmission buildPseudoSubmission(BasketMatch match, int homeScore, int awayScore) {
        BasketMatchDataSubmission s = new BasketMatchDataSubmission();
        s.setMatchId(match.getId());
        s.setSeasonId(match.getSeasonId());
        s.setStageId(match.getStageId());
        s.setHomeTeamId(match.getHomeTeamId());
        s.setAwayTeamId(match.getAwayTeamId());
        s.setHomeScore(homeScore);
        s.setAwayScore(awayScore);
        return s;
    }

    private List<BasketMatchDataSubmissionPeriod> toPeriods(Long matchId, MatchOfficialDataCorrectionReq req) {
        if (CollectionUtils.isEmpty(req.getPeriodScores())) {
            throw new ServiceException("分节比分不能为空");
        }
        List<BasketMatchDataSubmissionPeriod> list = new ArrayList<>();
        for (MatchOfficialDataCorrectionReq.PeriodItem item : req.getPeriodScores()) {
            BasketMatchDataSubmissionPeriod p = new BasketMatchDataSubmissionPeriod();
            p.setMatchId(matchId);
            p.setPeriodNo(item.getPeriodNo());
            p.setPeriodType(item.getPeriodType());
            p.setHomeScore(nvl(item.getHomeScore()));
            p.setAwayScore(nvl(item.getAwayScore()));
            list.add(p);
        }
        return list;
    }

    private List<BasketMatchDataSubmissionTeamStat> toTeamStats(BasketMatch match, MatchOfficialDataCorrectionReq req) {
        List<BasketMatchDataSubmissionTeamStat> list = new ArrayList<>();
        for (MatchOfficialDataCorrectionReq.TeamStatItem item : req.getTeamStats()) {
            BasketMatchDataSubmissionTeamStat stat = new BasketMatchDataSubmissionTeamStat();
            stat.setMatchId(match.getId());
            stat.setSeasonId(match.getSeasonId());
            stat.setStageId(match.getStageId());
            stat.setTeamId(item.getTeamId());
            Long opponent = item.getTeamId().equals(match.getHomeTeamId()) ? match.getAwayTeamId() : match.getHomeTeamId();
            stat.setOpponentTeamId(opponent);
            stat.setPoints(nvl(item.getPoints()));
            stat.setRebounds(nvl(item.getRebounds()));
            stat.setOffensiveRebounds(nvl(item.getOffensiveRebounds()));
            stat.setDefensiveRebounds(nvl(item.getDefensiveRebounds()));
            stat.setAssists(nvl(item.getAssists()));
            stat.setSteals(nvl(item.getSteals()));
            stat.setBlocks(nvl(item.getBlocks()));
            stat.setTurnovers(nvl(item.getTurnovers()));
            stat.setFouls(nvl(item.getFouls()));
            stat.setFieldGoalsMade(nvl(item.getFieldGoalsMade()));
            stat.setFieldGoalsAttempted(nvl(item.getFieldGoalsAttempted()));
            stat.setFieldGoalPct(computePct(item.getFieldGoalsMade(), item.getFieldGoalsAttempted()));
            stat.setThreePointsMade(nvl(item.getThreePointsMade()));
            stat.setThreePointsAttempted(nvl(item.getThreePointsAttempted()));
            stat.setThreePointPct(computePct(item.getThreePointsMade(), item.getThreePointsAttempted()));
            stat.setFreeThrowsMade(nvl(item.getFreeThrowsMade()));
            stat.setFreeThrowsAttempted(nvl(item.getFreeThrowsAttempted()));
            stat.setFreeThrowPct(computePct(item.getFreeThrowsMade(), item.getFreeThrowsAttempted()));
            list.add(stat);
        }
        return list;
    }

    private List<BasketMatchDataSubmissionPlayerStat> toPlayerStats(BasketMatch match, MatchOfficialDataCorrectionReq req) {
        List<BasketMatchDataSubmissionPlayerStat> list = new ArrayList<>();
        for (MatchOfficialDataCorrectionReq.PlayerStatItem item : req.getPlayerStats()) {
            BasketMatchDataSubmissionPlayerStat stat = new BasketMatchDataSubmissionPlayerStat();
            stat.setMatchId(match.getId());
            stat.setSeasonId(match.getSeasonId());
            stat.setStageId(match.getStageId());
            stat.setTeamId(item.getTeamId());
            stat.setPlayerId(item.getPlayerId());
            stat.setRosterId(item.getRosterId());
            stat.setStartedFlag(item.getStartedFlag() != null ? item.getStartedFlag() : 0);
            stat.setMinutesSeconds(nvl(item.getMinutesSeconds()));
            stat.setPoints(nvl(item.getPoints()));
            stat.setRebounds(nvl(item.getRebounds()));
            stat.setOffensiveRebounds(nvl(item.getOffensiveRebounds()));
            stat.setDefensiveRebounds(nvl(item.getDefensiveRebounds()));
            stat.setAssists(nvl(item.getAssists()));
            stat.setSteals(nvl(item.getSteals()));
            stat.setBlocks(nvl(item.getBlocks()));
            stat.setTurnovers(nvl(item.getTurnovers()));
            stat.setFouls(nvl(item.getFouls()));
            stat.setFieldGoalsMade(nvl(item.getFieldGoalsMade()));
            stat.setFieldGoalsAttempted(nvl(item.getFieldGoalsAttempted()));
            stat.setFieldGoalPct(computePct(item.getFieldGoalsMade(), item.getFieldGoalsAttempted()));
            stat.setThreePointsMade(nvl(item.getThreePointsMade()));
            stat.setThreePointsAttempted(nvl(item.getThreePointsAttempted()));
            stat.setThreePointPct(computePct(item.getThreePointsMade(), item.getThreePointsAttempted()));
            stat.setFreeThrowsMade(nvl(item.getFreeThrowsMade()));
            stat.setFreeThrowsAttempted(nvl(item.getFreeThrowsAttempted()));
            stat.setFreeThrowPct(computePct(item.getFreeThrowsMade(), item.getFreeThrowsAttempted()));
            list.add(stat);
        }
        return list;
    }

    private Long writeRevision(BasketMatch match, BasketMatchOfficialResult currentOfficial, String source,
                               Long disputeId, Long operatorId, String reason, int fromVersion, int toVersion, Date now) {
        BasketMatchDataRevision revision = new BasketMatchDataRevision();
        revision.setRevisionNo("REV-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        revision.setMatchId(match.getId());
        revision.setFromVersion(fromVersion);
        revision.setToVersion(toVersion);
        revision.setFromOfficialResultId(currentOfficial != null ? currentOfficial.getId() : null);
        revision.setRevisionReason(reason);
        revision.setRevisionSource(source);
        revision.setDisputeId(disputeId);
        revision.setOperatorId(operatorId);
        revision.setAuditorId(operatorId);
        revision.setEffectiveTime(now);
        revision.setRevisionStatus("EFFECTIVE");
        revision.setCreateTime(now);
        revision.setUpdateTime(now);
        revision.setIsDelete(NOT_DELETED);
        revisionMapper.insert(revision);
        return revision.getId();
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

    private void syncTeamStats(BasketMatch match, List<BasketMatchDataSubmissionTeamStat> teamStats,
                               Long operatorId, Date now) {
        List<BasketMatchTeamStat> list = new ArrayList<>();
        for (BasketMatchDataSubmissionTeamStat stat : teamStats) {
            BasketMatchTeamStat row = new BasketMatchTeamStat();
            row.setMatchId(match.getId());
            row.setSeasonId(match.getSeasonId());
            row.setStageId(match.getStageId());
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
            row.setPossessions(stat.getPossessions() != null ? stat.getPossessions() : BigDecimal.ZERO);
            row.setEfficiency(stat.getEfficiency() != null ? stat.getEfficiency() : BigDecimal.ZERO);
            row.setDataStatus(MatchDataStatusEnum.CONFIRMED.getCode());
            row.setConfirmedBy(operatorId);
            row.setConfirmedTime(now);
            row.setCreateTime(now);
            row.setUpdateTime(now);
            row.setIsDelete(NOT_DELETED);
            list.add(row);
        }
        teamStatMapper.softDeleteByMatchId(match.getId());
        teamStatMapper.batchInsert(list);
    }

    private void syncPlayerStats(BasketMatch match, List<BasketMatchDataSubmissionPlayerStat> playerStats,
                                 Long operatorId, Date now) {
        List<BasketMatchPlayerStat> list = new ArrayList<>();
        for (BasketMatchDataSubmissionPlayerStat stat : playerStats) {
            BasketMatchPlayerStat row = new BasketMatchPlayerStat();
            row.setMatchId(match.getId());
            row.setSeasonId(match.getSeasonId());
            row.setStageId(match.getStageId());
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
            row.setEfficiency(stat.getEfficiency() != null ? stat.getEfficiency() : BigDecimal.ZERO);
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
        playerStatMapper.softDeleteByMatchId(match.getId());
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

    private Long resolveWinnerId(BasketMatch match, int homeScore, int awayScore) {
        if (homeScore > awayScore) {
            return match.getHomeTeamId();
        }
        if (awayScore > homeScore) {
            return match.getAwayTeamId();
        }
        return null;
    }

    private void writeStatChange(BasketMatch match, String field, String before, String after, String reason) {
        Date now = new Date();
        BasketStatChangeRecord record = new BasketStatChangeRecord();
        record.setBizType(BIZ_TYPE_MATCH);
        record.setBizId(match.getId());
        record.setFieldName(field);
        record.setBeforeValue(before);
        record.setAfterValue(after);
        record.setChangeReason(reason);
        record.setOperatorId(SecurityUtils.getUserId());
        record.setOperatorName(SecurityUtils.getUsername());
        record.setApprovalStatus(APPROVAL_NONE);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        record.setIsDelete(NOT_DELETED);
        statChangeRecordMapper.insert(record);
    }

    private BigDecimal computePct(Integer made, Integer attempted) {
        int m = nvl(made);
        int a = nvl(attempted);
        if (a <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(m).divide(BigDecimal.valueOf(a), 4, RoundingMode.HALF_UP);
    }

    private int nvl(Integer value) {
        return value != null ? value : 0;
    }
}
