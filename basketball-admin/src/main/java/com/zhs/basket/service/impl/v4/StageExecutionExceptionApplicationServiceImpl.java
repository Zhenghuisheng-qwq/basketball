package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketDivisionStageQualification;
import com.zhs.basket.domain.v4.BasketDivisionStageTeam;
import com.zhs.basket.domain.v4.BasketMatchSeries;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.dto.v4.BasketAdminOfficialResultReq;
import com.zhs.basket.dto.v4.BasketAdminResultCorrectionReq;
import com.zhs.basket.dto.v4.BasketManualAdvanceReq;
import com.zhs.basket.dto.v4.BasketMatchForfeitReq;
import com.zhs.basket.dto.v4.BasketMatchForfeitRevokeReq;
import com.zhs.basket.dto.v4.BasketPairingAdjustReq;
import com.zhs.basket.dto.v4.BasketPairingSlotReq;
import com.zhs.basket.dto.v4.BasketSeriesForfeitReq;
import com.zhs.basket.dto.v4.BasketSeriesManualResolutionReq;
import com.zhs.basket.dto.v4.BasketSeriesRevokeResolutionReq;
import com.zhs.basket.req.AuditFieldInput;
import com.zhs.basket.support.v4.AdminOfficialResultCapability;
import com.zhs.basket.support.v4.schedule.WinnerOfSeriesAdvanceSupport;
import com.zhs.basket.support.v4.settlement.OfficialMatchResultCommand;
import com.zhs.basket.support.v4.settlement.OfficialMatchResultSettlementService;
import com.zhs.basket.enums.v4.OfficialResultSource;
import com.zhs.basket.enums.v4.MatchSeriesStatusEnum;
import com.zhs.basket.enums.v4.MatchStatusEnum;
import com.zhs.basket.enums.v4.PublishStatusEnum;
import com.zhs.basket.enums.v4.QualificationRecordStatusEnum;
import com.zhs.basket.enums.v4.QualificationTypeEnum;
import com.zhs.basket.enums.v4.SeriesResolutionTypeEnum;
import com.zhs.basket.enums.v4.StageExecutionStatusEnum;
import com.zhs.basket.mapper.v4.BasketDivisionStageMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageQualificationMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageTeamMapper;
import com.zhs.basket.mapper.v4.BasketMatchSeriesMapper;
import com.zhs.basket.mapper.v4.BasketScheduleBatchMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.service.v4.IBasketMatchSeriesService;
import com.zhs.basket.service.v4.IDivisionStageExecutionApplicationService;
import com.zhs.basket.service.v4.IStageExecutionExceptionApplicationService;
import com.zhs.basket.support.BasketBusinessAuditWriter;
import com.zhs.basket.support.v4.ByeQualificationService;
import com.zhs.basket.support.v4.MatchSeriesExecutionCapability;
import com.zhs.basket.support.v4.MatchSeriesQualificationSyncService;
import com.zhs.basket.support.v4.MatchSeriesResultSnapshot;
import com.zhs.basket.support.v4.stage.EliminationStageSupport;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class StageExecutionExceptionApplicationServiceImpl implements IStageExecutionExceptionApplicationService {

    private static final int NOT_DELETED = 0;

    @Autowired
    private BasketV4MatchMapper matchMapper;
    @Autowired
    private BasketMatchSeriesMapper seriesMapper;
    @Autowired
    private BasketDivisionStageMapper stageMapper;
    @Autowired
    private BasketDivisionStageTeamMapper stageTeamMapper;
    @Autowired
    private BasketDivisionStageQualificationMapper qualificationMapper;
    @Autowired
    private BasketScheduleBatchMapper scheduleBatchMapper;
    @Autowired
    private MatchSeriesQualificationSyncService qualificationSyncService;
    @Autowired
    private IBasketMatchSeriesService matchSeriesService;
    @Autowired
    private IDivisionStageExecutionApplicationService stageExecutionService;
    @Autowired
    private BasketBusinessAuditWriter auditWriter;
    @Autowired
    private OfficialMatchResultSettlementService officialMatchResultSettlementService;
    @Autowired
    private WinnerOfSeriesAdvanceSupport winnerOfSeriesAdvanceSupport;
    @Autowired
    private com.zhs.basket.support.v4.EventTeamParticipationGuard eventTeamParticipationGuard;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forfeitMatch(BasketMatchForfeitReq req) {
        requireReason(req.getReason());
        BasketV4Match match = requireMatch(req.getMatchId());
        if (!AdminOfficialResultCapability.canForfeitMatch(match)) {
            throw new ServiceException("当前比赛不能判负");
        }
        Long loser = req.getLoserEventTeamId();
        if (loser == null || (!loser.equals(match.getHomeEventTeamId()) && !loser.equals(match.getAwayEventTeamId()))) {
            throw new ServiceException("判负球队必须是比赛双方之一");
        }
        Long winner = loser.equals(match.getHomeEventTeamId()) ? match.getAwayEventTeamId() : match.getHomeEventTeamId();
        if (match.getSeriesId() != null) {
            BasketMatchSeries series = requireSeries(match.getSeriesId());
            if (MatchSeriesStatusEnum.COMPLETED.getCode().equals(series.getSeriesStatus())) {
                winnerOfSeriesAdvanceSupport.assertDownstreamNotStarted(series);
            }
        }
        OfficialMatchResultCommand command = new OfficialMatchResultCommand();
        command.setMatchId(match.getId());
        command.setHomeScore(null);
        command.setAwayScore(null);
        command.setWinnerEventTeamId(winner);
        command.setLoserEventTeamId(loser);
        command.setMatchStatus(MatchStatusEnum.FORFEITED.getCode());
        command.setSource(OfficialResultSource.FORFEIT);
        command.setRemark(req.getReason());
        command.setClearOfficialPlayerStats(true);
        if (!officialMatchResultSettlementService.settle(command)) {
            return;
        }
        auditWriter.writeAudit("V4_STAGE", "MATCH", match.getId(), "FORFEIT",
                "比赛判负/弃赛", req.getReason(), List.of(
                        AuditFieldInput.of("resultSource", "结果来源", null, OfficialResultSource.FORFEIT.getCode()),
                        AuditFieldInput.of("winnerEventTeamId", "胜者", null, winner),
                        AuditFieldInput.of("loserEventTeamId", "判负方", null, loser),
                        AuditFieldInput.of("matchStatus", "比赛状态", match.getMatchStatus(), MatchStatusEnum.FORFEITED.getCode())
                ));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeMatchForfeit(BasketMatchForfeitRevokeReq req) {
        requireReason(req.getReason());
        BasketV4Match match = requireMatch(req.getMatchId());
        if (!AdminOfficialResultCapability.canRevokeMatchForfeit(match)) {
            throw new ServiceException("当前比赛不能撤销判负");
        }
        if (match.getSeriesId() != null) {
            BasketMatchSeries series = requireSeries(match.getSeriesId());
            winnerOfSeriesAdvanceSupport.assertDownstreamNotStarted(series);
        } else {
            BasketDivisionStage stage = requireStage(match.getStageId());
            qualificationSyncService.assertCanModifyDownstream(stage);
        }
        String oldStatus = match.getMatchStatus();
        Long oldWinner = match.getWinnerEventTeamId();
        officialMatchResultSettlementService.unsetOfficialResult(match.getId());
        auditWriter.writeAudit("V4_STAGE", "MATCH", match.getId(), "REVOKE_MATCH_FORFEIT",
                "撤销比赛判负", req.getReason(), List.of(
                        AuditFieldInput.of("matchId", "比赛", null, match.getId()),
                        AuditFieldInput.of("matchStatus", "比赛状态", oldStatus, MatchStatusEnum.SCHEDULED.getCode()),
                        AuditFieldInput.of("winnerEventTeamId", "原胜者", oldWinner, null)
                ));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forfeitSeries(BasketSeriesForfeitReq req) {
        requireReason(req.getReason());
        BasketMatchSeries series = requireSeries(req.getSeriesId());
        if (SeriesResolutionTypeEnum.FORFEIT.getCode().equals(series.getResolutionType())) {
            if (req.getForfeitEventTeamId().equals(series.getHomeEventTeamId())
                    || req.getForfeitEventTeamId().equals(series.getAwayEventTeamId())) {
                return;
            }
            throw new ServiceException("系列赛已判定弃赛，无法变更弃赛方");
        }
        if (!MatchSeriesStatusEnum.COMPLETED.getCode().equals(series.getSeriesStatus())
                && !MatchSeriesStatusEnum.RUNNING.getCode().equals(series.getSeriesStatus())
                && !MatchSeriesStatusEnum.READY.getCode().equals(series.getSeriesStatus())) {
            throw new ServiceException("当前系列赛状态不允许判定弃赛");
        }
        Long forfeitTeam = req.getForfeitEventTeamId();
        if (forfeitTeam == null
                || (!forfeitTeam.equals(series.getHomeEventTeamId()) && !forfeitTeam.equals(series.getAwayEventTeamId()))) {
            throw new ServiceException("弃赛球队必须是系列赛双方之一");
        }
        BasketDivisionStage stage = requireStage(series.getStageId());
        qualificationSyncService.assertCanModifyDownstream(stage);
        winnerOfSeriesAdvanceSupport.assertDownstreamNotStarted(series);

        Long winner = forfeitTeam.equals(series.getHomeEventTeamId())
                ? series.getAwayEventTeamId() : series.getHomeEventTeamId();
        applyUnstartedSeriesSweepScore(series, winner);
        Date now = new Date();
        Long operatorId = SecurityUtils.getUserId();
        series.setWinnerEventTeamId(winner);
        series.setSeriesStatus(MatchSeriesStatusEnum.COMPLETED.getCode());
        series.setResolutionType(SeriesResolutionTypeEnum.FORFEIT.getCode());
        series.setResolutionReason(req.getReason().trim());
        series.setResolvedTime(now);
        series.setResolvedBy(operatorId);
        series.setUpdateTime(now);
        if (seriesMapper.updateResolution(series) != 1) {
            throw new ServiceException("更新系列赛弃赛结果失败");
        }

        MatchSeriesResultSnapshot snapshot = new MatchSeriesResultSnapshot();
        snapshot.setHomeWinCount(series.getHomeWinCount() != null ? series.getHomeWinCount() : 0);
        snapshot.setAwayWinCount(series.getAwayWinCount() != null ? series.getAwayWinCount() : 0);
        snapshot.setWinnerEventTeamId(winner);
        snapshot.setCompleted(true);
        qualificationSyncService.syncAfterSeriesRebuild(series,
                MatchSeriesStatusEnum.RUNNING.getCode(), null, snapshot);
        winnerOfSeriesAdvanceSupport.rewireAfterSeriesResult(series);
        stageExecutionService.checkStageCompletion(series.getStageId());
        auditWriter.writeAudit("V4_STAGE", "MATCH_SERIES", series.getId(), "SERIES_FORFEIT",
                "系列赛弃赛", req.getReason(), List.of(
                        AuditFieldInput.of("resultSource", "结果来源", null, SeriesResolutionTypeEnum.FORFEIT.getCode()),
                        AuditFieldInput.of("winnerEventTeamId", "系列赛胜者", null, winner),
                        AuditFieldInput.of("forfeitEventTeamId", "弃赛方", null, forfeitTeam)
                ));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualAdvance(BasketManualAdvanceReq req) {
        requireReason(req.getReason());
        BasketDivisionStage stage = requireStage(req.getStageId());
        qualificationSyncService.assertCanModifyDownstream(stage);
        if (!EliminationStageSupport.isWinnerAdvanceNonFinalStage(stage)
                && !EliminationStageSupport.isSingleRoundNonFinalStage(stage)) {
            throw new ServiceException("当前阶段不支持人工直接晋级");
        }
        if (!stageTeamMapper.existsByStageAndEventTeam(stage.getId(), req.getEventTeamId())) {
            throw new ServiceException("球队不在当前阶段参赛名单中");
        }
        eventTeamParticipationGuard.assertCanParticipate(req.getEventTeamId());
        BasketDivisionStageQualification existing = qualificationMapper.selectActiveByStageAndTeamAndType(
                stage.getId(), req.getEventTeamId(), QualificationTypeEnum.MANUAL.getCode());
        if (existing != null) {
            return;
        }
        Long targetStageId = resolveTargetStageId(stage);
        Date now = new Date();
        BasketDivisionStageQualification row = new BasketDivisionStageQualification();
        row.setDivisionId(stage.getDivisionId());
        row.setStageId(stage.getId());
        row.setEventTeamId(req.getEventTeamId());
        row.setQualificationType(QualificationTypeEnum.MANUAL.getCode());
        row.setTargetStageId(targetStageId);
        row.setQualificationStatus(QualificationRecordStatusEnum.PREVIEW.getCode());
        row.setRemark(req.getReason().trim());
        row.setActiveUniqueKey(stage.getId() + ":MANUAL:PREVIEW:" + req.getEventTeamId());
        row.setCreateTime(now);
        row.setUpdateTime(now);
        row.setIsDelete(NOT_DELETED);
        if (qualificationMapper.insert(row) != 1) {
            throw new ServiceException("写入人工晋级预览失败");
        }
        auditWriter.writeAudit("V4_STAGE", "STAGE", stage.getId(), "MANUAL_ADVANCE",
                "人工直接晋级", req.getReason(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustPairing(BasketPairingAdjustReq req) {
        requireReason(req.getReason());
        BasketDivisionStage stage = requireStage(req.getStageId());
        if (stage.getScheduleBatchId() == null) {
            throw new ServiceException("阶段尚未生成赛程，请先预览后生成");
        }
        BasketScheduleBatch batch = scheduleBatchMapper.selectById(stage.getScheduleBatchId());
        if (batch == null || !PublishStatusEnum.DRAFT.getCode().equals(batch.getPublishStatus())) {
            throw new ServiceException("仅草稿赛程可调整对阵，已发布请作废后重新生成");
        }
        if (!StageExecutionStatusEnum.SCHEDULE_GENERATED.getCode().equals(stage.getExecutionStatus())) {
            throw new ServiceException("当前阶段状态不允许调整对阵");
        }
        List<BasketPairingSlotReq> slots = req.getSlots();
        if (slots == null || slots.isEmpty()) {
            throw new ServiceException("请提供对阵明细");
        }
        List<BasketDivisionStageTeam> stageTeams = stageTeamMapper.selectByStageId(stage.getId());
        Set<Long> teamIds = new HashSet<>();
        for (BasketDivisionStageTeam t : stageTeams) {
            teamIds.add(t.getEventTeamId());
        }
        Set<Long> used = new HashSet<>();
        for (BasketPairingSlotReq slot : slots) {
            if (slot.getHomeEventTeamId() == null || slot.getAwayEventTeamId() == null) {
                throw new ServiceException("对阵双方不能为空");
            }
            if (slot.getHomeEventTeamId().equals(slot.getAwayEventTeamId())) {
                throw new ServiceException("对阵双方不能相同");
            }
            if (!teamIds.contains(slot.getHomeEventTeamId()) || !teamIds.contains(slot.getAwayEventTeamId())) {
                throw new ServiceException("对阵球队必须来自当前阶段参赛名单");
            }
            if (!used.add(slot.getHomeEventTeamId()) || !used.add(slot.getAwayEventTeamId())) {
                throw new ServiceException("每支球队只能出现一次（轮空队除外）");
            }
        }
        matchMapper.softDeleteDraftByBatchId(batch.getId());
        seriesMapper.softDeleteDraftByBatchId(batch.getId());
        qualificationMapper.softDeletePreviewByStageId(stage.getId());

        boolean seriesStage = MatchSeriesExecutionCapability.supportsScheduleGeneration(stage);
        Date now = new Date();
        if (seriesStage) {
            regenerateSeriesPairing(stage, batch, slots, now);
        } else {
            regenerateBo1Pairing(stage, batch, slots, now);
        }
        auditWriter.writeAudit("V4_STAGE", "STAGE", stage.getId(), "PAIRING_ADJUST",
                "人工调整对阵", req.getReason(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeSeriesResolution(BasketSeriesRevokeResolutionReq req) {
        requireReason(req.getReason());
        BasketMatchSeries series = requireSeries(req.getSeriesId());
        if (!SeriesResolutionTypeEnum.FORFEIT.getCode().equals(series.getResolutionType())
                && !SeriesResolutionTypeEnum.MANUAL.getCode().equals(series.getResolutionType())) {
            throw new ServiceException("当前系列赛无异常判定，无需撤销");
        }
        BasketDivisionStage stage = requireStage(series.getStageId());
        qualificationSyncService.assertCanModifyDownstream(stage);
        winnerOfSeriesAdvanceSupport.assertDownstreamNotStarted(series);

        Long previousWinner = series.getWinnerEventTeamId();
        qualificationMapper.softDeleteActiveBySourceSeriesId(series.getId());
        if (seriesMapper.clearResolution(series.getId()) != 1) {
            throw new ServiceException("撤销系列赛异常判定失败");
        }
        matchSeriesService.rebuildSeriesResult(series.getId());
        stageExecutionService.checkStageCompletion(series.getStageId());
        auditWriter.writeAudit("V4_STAGE", "MATCH_SERIES", series.getId(), "REVOKE_SERIES_RESOLUTION",
                "撤销系列赛异常判定", req.getReason(), List.of(
                        AuditFieldInput.of("resolutionType", "裁决类型", series.getResolutionType(), null),
                        AuditFieldInput.of("winnerEventTeamId", "原系列赛胜者", previousWinner, null)
                ));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enterOfficialResult(BasketAdminOfficialResultReq req) {
        requireReason(req.getReason());
        requireScores(req.getHomeScore(), req.getAwayScore());
        BasketV4Match match = requireMatch(req.getMatchId());
        if (!AdminOfficialResultCapability.canEnterOfficialResult(match)) {
            throw new ServiceException("当前比赛不能管理员录入正式结果");
        }
        Long winner = OfficialMatchResultSettlementService.resolveWinner(
                match.getHomeEventTeamId(), match.getAwayEventTeamId(), req.getHomeScore(), req.getAwayScore());
        OfficialMatchResultCommand command = scoredCommand(match, req.getHomeScore(), req.getAwayScore(),
                OfficialResultSource.ADMIN_DIRECT, req.getRemark());
        if (!officialMatchResultSettlementService.settle(command)) {
            return;
        }
        auditWriter.writeAudit("V4_STAGE", "MATCH", match.getId(), "ADMIN_DIRECT_RESULT",
                "管理员录入正式结果", req.getReason(), List.of(
                        AuditFieldInput.of("resultSource", "结果来源", null, OfficialResultSource.ADMIN_DIRECT.getCode()),
                        AuditFieldInput.of("homeScore", "主队比分", null, req.getHomeScore()),
                        AuditFieldInput.of("awayScore", "客队比分", null, req.getAwayScore()),
                        AuditFieldInput.of("winnerEventTeamId", "胜者", null, winner),
                        AuditFieldInput.of("matchStatus", "比赛状态", match.getMatchStatus(), MatchStatusEnum.FINISHED.getCode())
                ));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void correctOfficialResult(BasketAdminResultCorrectionReq req) {
        requireReason(req.getReason());
        requireScores(req.getHomeScore(), req.getAwayScore());
        BasketV4Match match = requireMatch(req.getMatchId());
        if (!AdminOfficialResultCapability.canCorrectOfficialResult(match)) {
            throw new ServiceException("当前比赛不能修正正式结果");
        }
        if (match.getSeriesId() != null) {
            BasketMatchSeries series = requireSeries(match.getSeriesId());
            if (MatchSeriesStatusEnum.COMPLETED.getCode().equals(series.getSeriesStatus())) {
                winnerOfSeriesAdvanceSupport.assertDownstreamNotStarted(series);
            }
        }
        Long newWinner = OfficialMatchResultSettlementService.resolveWinner(
                match.getHomeEventTeamId(), match.getAwayEventTeamId(), req.getHomeScore(), req.getAwayScore());
        OfficialMatchResultCommand command = scoredCommand(match, req.getHomeScore(), req.getAwayScore(),
                OfficialResultSource.ADMIN_CORRECTION, req.getRemark());
        if (!officialMatchResultSettlementService.settle(command)) {
            return;
        }
        auditWriter.writeAudit("V4_STAGE", "MATCH", match.getId(), "RESULT_CORRECTION",
                "管理员修正正式结果", req.getReason(), List.of(
                        AuditFieldInput.of("resultSource", "结果来源", OfficialResultSource.SUBMISSION.getCode(),
                                OfficialResultSource.ADMIN_CORRECTION.getCode()),
                        AuditFieldInput.of("homeScore", "主队比分", match.getHomeScore(), req.getHomeScore()),
                        AuditFieldInput.of("awayScore", "客队比分", match.getAwayScore(), req.getAwayScore()),
                        AuditFieldInput.of("winnerEventTeamId", "胜者", match.getWinnerEventTeamId(), newWinner),
                        AuditFieldInput.of("matchStatus", "比赛状态", match.getMatchStatus(), MatchStatusEnum.FINISHED.getCode())
                ));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolveSeriesManually(BasketSeriesManualResolutionReq req) {
        requireReason(req.getReason());
        BasketMatchSeries series = requireSeries(req.getSeriesId());
        if (!AdminOfficialResultCapability.canResolveSeriesManually(series)) {
            throw new ServiceException("当前系列赛不能管理员裁决");
        }
        Long winner = req.getWinnerEventTeamId();
        if (winner == null || (!winner.equals(series.getHomeEventTeamId()) && !winner.equals(series.getAwayEventTeamId()))) {
            throw new ServiceException("裁决胜方必须是系列赛双方之一");
        }
        String decisionType = normalizeDecisionType(req.getDecisionType());
        BasketDivisionStage stage = requireStage(series.getStageId());
        qualificationSyncService.assertCanModifyDownstream(stage);
        winnerOfSeriesAdvanceSupport.assertDownstreamNotStarted(series);

        if (SeriesResolutionTypeEnum.MANUAL.getCode().equals(series.getResolutionType())
                && winner.equals(series.getWinnerEventTeamId())
                && MatchSeriesStatusEnum.COMPLETED.getCode().equals(series.getSeriesStatus())) {
            return;
        }

        Date now = new Date();
        Long previousWinner = series.getWinnerEventTeamId();
        series.setWinnerEventTeamId(winner);
        series.setSeriesStatus(MatchSeriesStatusEnum.COMPLETED.getCode());
        series.setResolutionType(SeriesResolutionTypeEnum.MANUAL.getCode());
        series.setResolutionReason(decisionType + "：" + req.getReason().trim());
        series.setResolvedTime(now);
        series.setResolvedBy(SecurityUtils.getUserId());
        series.setUpdateTime(now);
        if (seriesMapper.updateResolution(series) != 1) {
            throw new ServiceException("更新系列赛裁决失败");
        }

        MatchSeriesResultSnapshot snapshot = new MatchSeriesResultSnapshot();
        snapshot.setHomeWinCount(series.getHomeWinCount() != null ? series.getHomeWinCount() : 0);
        snapshot.setAwayWinCount(series.getAwayWinCount() != null ? series.getAwayWinCount() : 0);
        snapshot.setWinnerEventTeamId(winner);
        snapshot.setCompleted(true);
        qualificationSyncService.syncAfterSeriesRebuild(series,
                MatchSeriesStatusEnum.RUNNING.getCode(), previousWinner, snapshot);
        winnerOfSeriesAdvanceSupport.rewireAfterSeriesResult(series);
        stageExecutionService.checkStageCompletion(series.getStageId());
        auditWriter.writeAudit("V4_STAGE", "MATCH_SERIES", series.getId(), "SERIES_MANUAL_RESOLUTION",
                "系列赛管理员裁决", req.getReason(), List.of(
                        AuditFieldInput.of("resultSource", "结果来源", series.getResolutionType(),
                                SeriesResolutionTypeEnum.MANUAL.getCode()),
                        AuditFieldInput.of("decisionType", "裁决类型", null, decisionType),
                        AuditFieldInput.of("winnerEventTeamId", "系列赛胜者", previousWinner, winner)
                ));
    }

    private OfficialMatchResultCommand scoredCommand(BasketV4Match match, Integer homeScore, Integer awayScore,
                                                     OfficialResultSource source, String remark) {
        Long winner = OfficialMatchResultSettlementService.resolveWinner(
                match.getHomeEventTeamId(), match.getAwayEventTeamId(), homeScore, awayScore);
        OfficialMatchResultCommand command = new OfficialMatchResultCommand();
        command.setMatchId(match.getId());
        command.setHomeScore(homeScore);
        command.setAwayScore(awayScore);
        command.setWinnerEventTeamId(winner);
        command.setLoserEventTeamId(OfficialMatchResultSettlementService.resolveLoser(
                match.getHomeEventTeamId(), match.getAwayEventTeamId(), winner));
        command.setMatchStatus(MatchStatusEnum.FINISHED.getCode());
        command.setSource(source);
        command.setRemark(remark);
        command.setClearOfficialPlayerStats(true);
        command.setScoreOnlyTeamStats(true);
        return command;
    }

    private void requireScores(Integer homeScore, Integer awayScore) {
        if (homeScore == null || awayScore == null) {
            throw new ServiceException("主客队比分不能为空");
        }
        if (homeScore < 0 || awayScore < 0) {
            throw new ServiceException("比分不能为负数");
        }
    }

    private String normalizeDecisionType(String decisionType) {
        if (decisionType == null || decisionType.isBlank()) {
            return "其他";
        }
        String code = decisionType.trim().toUpperCase();
        if ("ARBITRATION".equals(code) || "赛事仲裁".equals(decisionType.trim())) {
            return "赛事仲裁";
        }
        if ("VIOLATION".equals(code) || "违规判定".equals(decisionType.trim())) {
            return "违规判定";
        }
        return "其他";
    }

    private void regenerateBo1Pairing(BasketDivisionStage stage, BasketScheduleBatch batch,
                                        List<BasketPairingSlotReq> slots, Date now) {
        for (BasketPairingSlotReq slot : slots) {
            int bracketSlot = slot.getBracketSlot() == null ? 0 : slot.getBracketSlot();
            BasketV4Match match = new BasketV4Match();
            match.setSeasonId(batch.getSeasonId());
            match.setDivisionId(stage.getDivisionId());
            match.setStageId(stage.getId());
            match.setStageCode(stage.getStageCode());
            match.setScheduleBatchId(batch.getId());
            match.setRoundNo(1);
            match.setBracketSlot(bracketSlot);
            match.setMatchNo(buildAdjustedMatchNo(stage, bracketSlot, 1));
            match.setHomeEventTeamId(slot.getHomeEventTeamId());
            match.setAwayEventTeamId(slot.getAwayEventTeamId());
            match.setMatchStatus(MatchStatusEnum.SCHEDULED.getCode());
            match.setPublishStatus(PublishStatusEnum.DRAFT.getCode());
            match.setCreateTime(now);
            match.setUpdateTime(now);
            match.setIsDelete(NOT_DELETED);
            matchMapper.insert(match);
        }
    }

    private void regenerateSeriesPairing(BasketDivisionStage stage, BasketScheduleBatch batch,
                                         List<BasketPairingSlotReq> slots, Date now) {
        int bestOf = stage.getBestOf();
        for (BasketPairingSlotReq slot : slots) {
            int bracketSlot = slot.getBracketSlot() == null ? 0 : slot.getBracketSlot();
            BasketMatchSeries series = new BasketMatchSeries();
            series.setDivisionId(stage.getDivisionId());
            series.setStageId(stage.getId());
            series.setScheduleBatchId(batch.getId());
            series.setSeriesCode(com.zhs.basket.support.v4.stage.MatchSeriesEliminationScheduleGenerator
                    .buildSeriesCode(stage.getDivisionId(), stage.getId(), 1, bracketSlot));
            series.setRoundNo(1);
            series.setBracketSlot(bracketSlot);
            series.setHomeEventTeamId(slot.getHomeEventTeamId());
            series.setAwayEventTeamId(slot.getAwayEventTeamId());
            series.setBestOf(bestOf);
            series.setHomeWinCount(0);
            series.setAwayWinCount(0);
            series.setSeriesStatus(MatchSeriesStatusEnum.READY.getCode());
            series.setCreateTime(now);
            series.setUpdateTime(now);
            series.setIsDelete(NOT_DELETED);
            seriesMapper.insert(series);
            for (int gameNo = 1; gameNo <= bestOf; gameNo++) {
                BasketV4Match match = new BasketV4Match();
                match.setSeasonId(batch.getSeasonId());
                match.setDivisionId(stage.getDivisionId());
                match.setStageId(stage.getId());
                match.setStageCode(stage.getStageCode());
                match.setScheduleBatchId(batch.getId());
                match.setSeriesId(series.getId());
                match.setSeriesGameNo(gameNo);
                match.setRoundNo(1);
                match.setBracketSlot(bracketSlot);
                match.setMatchNo(buildAdjustedMatchNo(stage, bracketSlot, gameNo));
                match.setHomeEventTeamId(slot.getHomeEventTeamId());
                match.setAwayEventTeamId(slot.getAwayEventTeamId());
                match.setMatchStatus(MatchStatusEnum.SCHEDULED.getCode());
                match.setPublishStatus(PublishStatusEnum.DRAFT.getCode());
                match.setCreateTime(now);
                match.setUpdateTime(now);
                match.setIsDelete(NOT_DELETED);
                matchMapper.insert(match);
            }
        }
    }

    private static String buildAdjustedMatchNo(BasketDivisionStage stage, int bracketSlot, int gameNo) {
        return "V4-" + stage.getDivisionId() + "-ADJ-" + stage.getId()
                + "-S" + bracketSlot + "-G" + gameNo + "-" + System.currentTimeMillis();
    }

    private void applyUnstartedSeriesSweepScore(BasketMatchSeries series, Long winner) {
        int required = requiredWins(series.getBestOf());
        if (winner != null && winner.equals(series.getHomeEventTeamId())) {
            series.setHomeWinCount(required);
            series.setAwayWinCount(0);
        } else {
            series.setHomeWinCount(0);
            series.setAwayWinCount(required);
        }
    }

    private static int requiredWins(Integer bestOf) {
        int bo = bestOf == null || bestOf < 1 ? 1 : bestOf;
        return (bo / 2) + 1;
    }

    private void requireReason(String reason) {
        if (StringUtils.isEmpty(reason) || reason.trim().length() < 2) {
            throw new ServiceException("请填写操作原因");
        }
    }

    private BasketV4Match requireMatch(Long id) {
        if (id == null) {
            throw new ServiceException("matchId 不能为空");
        }
        BasketV4Match match = matchMapper.selectById(id);
        if (match == null) {
            throw new ServiceException("比赛不存在");
        }
        return match;
    }

    private BasketMatchSeries requireSeries(Long id) {
        if (id == null) {
            throw new ServiceException("seriesId 不能为空");
        }
        BasketMatchSeries series = seriesMapper.selectById(id);
        if (series == null) {
            throw new ServiceException("系列赛不存在");
        }
        return series;
    }

    private BasketDivisionStage requireStage(Long id) {
        if (id == null) {
            throw new ServiceException("stageId 不能为空");
        }
        BasketDivisionStage stage = stageMapper.selectById(id);
        if (stage == null) {
            throw new ServiceException("阶段不存在");
        }
        return stage;
    }

    private Long resolveTargetStageId(BasketDivisionStage stage) {
        if (stage.getNextStageCode() == null || stage.getNextStageCode().isEmpty()) {
            return null;
        }
        return stageMapper.selectByDivisionId(stage.getDivisionId()).stream()
                .filter(s -> stage.getNextStageCode().equals(s.getStageCode()))
                .map(BasketDivisionStage::getId)
                .findFirst()
                .orElse(null);
    }
}
