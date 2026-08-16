package com.zhs.basket.support.penalty;

import com.zhs.basket.domain.BasketDisciplineExecutionDetail;
import com.zhs.basket.domain.BasketDisciplineRecord;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketDivisionStageQualification;
import com.zhs.basket.domain.v4.BasketEventTeam;
import com.zhs.basket.domain.v4.BasketMatchSeries;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.dto.v4.BasketMatchForfeitReq;
import com.zhs.basket.dto.v4.BasketSeriesForfeitReq;
import com.zhs.basket.enums.DisciplinePenaltyCodeEnum;
import com.zhs.basket.enums.PenaltyExecutionActionTypeEnum;
import com.zhs.basket.enums.PenaltyExecutionStatusEnum;
import com.zhs.basket.enums.v4.EventTeamStatusEnum;
import com.zhs.basket.enums.v4.MatchSeriesStatusEnum;
import com.zhs.basket.enums.v4.MatchStatusEnum;
import com.zhs.basket.mapper.BasketDisciplineExecutionDetailMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageQualificationMapper;
import com.zhs.basket.mapper.v4.BasketEventTeamMapper;
import com.zhs.basket.mapper.v4.BasketMatchSeriesMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.req.AuditFieldInput;
import com.zhs.basket.service.v4.IBasketV4StatsSettlementService;
import com.zhs.basket.service.v4.IStageExecutionExceptionApplicationService;
import com.zhs.basket.support.BasketBusinessAuditWriter;
import com.zhs.basket.support.v4.AdminOfficialResultCapability;
import com.zhs.basket.support.v4.schedule.WinnerOfSeriesAdvanceSupport;
import com.zhs.basket.vo.PenaltyExecutionResultVo;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Component
public class EventDisqualificationExecutionStrategy implements PenaltyExecutionStrategy {

    public static final String REASON = "\u8d5b\u4e8b\u8d44\u683c\u53d6\u6d88";
    public static final String MSG_DOWNSTREAM_STARTED_ADMIN =
            "\u88ab\u53d6\u6d88\u8d44\u683c\u7403\u961f\u5df2\u8fdb\u5165\u540e\u7eed\u8f6e\u6b21\u4e14\u6b63\u5f0f\u6bd4\u8d5b\u5df2\u5f00\u59cb\uff0c\u8bf7\u7ba1\u7406\u5458\u5f02\u5e38\u88c1\u5b9a\u3002";
    public static final String MSG_REQUIRES_ADMIN_ADJUDICATION =
            "\u88ab\u53d6\u6d88\u8d44\u683c\u7403\u961f\u5df2\u4ece\u5c1a\u672a\u5f00\u59cb\u7684\u4e0b\u6e38\u5bf9\u9635\u79fb\u9664\u3002\u5f53\u524d\u6ca1\u6709\u660e\u786e\u66ff\u8865\u664b\u7ea7\u89c4\u5219\uff0c\u8bf7\u7ba1\u7406\u5458\u88c1\u5b9a\u7a7a\u4f4d\u3002";
    private static final String DETAIL_SUCCESS = "SUCCESS";
    private static final String DETAIL_FAILED = "FAILED";
    private static final String TARGET_EVENT_TEAM = "EVENT_TEAM";
    private static final String TARGET_MATCH = "MATCH";
    private static final String TARGET_SERIES = "MATCH_SERIES";
    private static final String TARGET_STAGE = "STAGE";

    @Autowired
    private com.zhs.basket.mapper.BasketDisciplineRecordMapper disciplineRecordMapper;
    @Autowired
    private BasketDisciplineExecutionDetailMapper executionDetailMapper;
    @Autowired
    private BasketEventTeamMapper eventTeamMapper;
    @Autowired
    private BasketV4MatchMapper matchMapper;
    @Autowired
    private BasketMatchSeriesMapper seriesMapper;
    @Autowired
    private BasketDivisionStageMapper stageMapper;
    @Autowired
    private BasketDivisionStageQualificationMapper qualificationMapper;
    @Autowired
    private IStageExecutionExceptionApplicationService exceptionService;
    @Autowired
    private IBasketV4StatsSettlementService statsSettlementService;
    @Autowired
    private BasketBusinessAuditWriter auditWriter;
    @Autowired
    private WinnerOfSeriesAdvanceSupport winnerOfSeriesAdvanceSupport;

    @Override
    public boolean supports(BasketDisciplineRecord record) {
        return record != null
                && DisciplinePenaltyCodeEnum.EVENT_DISQUALIFICATION.name().equals(record.getPenaltyCode());
    }

    @Override
    public PenaltyExecutionResultVo execute(BasketDisciplineRecord record) {
        PenaltyExecutionResultVo result = new PenaltyExecutionResultVo();
        result.setExecutedTime(new Date());
        BasketEventTeam eventTeam;
        try {
            eventTeam = resolveEventTeam(record);
        } catch (ServiceException ex) {
            result.setExecutionStatus(PenaltyExecutionStatusEnum.FAILED.name());
            result.setErrorMessage(ex.getMessage());
            return result;
        }
        result.setEventTeamId(eventTeam.getId());
        result.setEventTeamName(eventTeam.getTeamNameSnapshot());
        result.setDivisionId(eventTeam.getDivisionId());
        result.setSeasonId(eventTeam.getSeasonId());

        disqualifyEventTeam(record, eventTeam, result);
        handleCompletedWinnerAdvance(record, eventTeam, result);
        forfeitFutureStandaloneMatches(record, eventTeam, result);
        forfeitOpenSeries(record, eventTeam, result);
        handleQualification(record, eventTeam, result);
        rebuildStanding(eventTeam, result);
        summarize(result);

        auditWriter.writeAudit("DISCIPLINE", "DISCIPLINE_RECORD", record.getId(), "PENALTY_EFFECTIVE",
                "\u53d6\u6d88\u8d5b\u4e8b\u8d44\u683c\u81ea\u52a8\u5904\u7406\u5b8c\u6210", REASON, List.of(
                        AuditFieldInput.of("executionStatus", "\u6267\u884c\u72b6\u6001", null, result.getExecutionStatus()),
                        AuditFieldInput.of("futureMatchForfeit", "\u672a\u6765\u6bd4\u8d5b\u5224\u8d1f", null, result.getFutureMatchForfeitCount()),
                        AuditFieldInput.of("seriesForfeit", "\u7cfb\u5217\u8d5b\u5224\u8d1f", null, result.getSeriesForfeitCount()),
                        AuditFieldInput.of("qualificationRemoved", "\u664b\u7ea7\u9884\u89c8\u79fb\u9664", null, result.getQualificationRemovedCount())
                ));
        return result;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void restoreEligibility(BasketDisciplineRecord record) {
        BasketEventTeam eventTeam;
        try {
            eventTeam = resolveEventTeam(record);
        } catch (ServiceException ex) {
            return;
        }
        if (!EventTeamStatusEnum.isDisqualified(eventTeam.getEventTeamStatus())) {
            return;
        }
        eventTeam.setEventTeamStatus(EventTeamStatusEnum.ACTIVE.getCode());
        eventTeamMapper.updateById(eventTeam);
        auditWriter.writeAudit("DISCIPLINE", "EVENT_TEAM", eventTeam.getId(), "EVENT_TEAM_ELIGIBILITY_RESTORED",
                "\u64a4\u9500\u5904\u7f5a\u540e\u6062\u590d\u8d5b\u4e8b\u53c2\u8d5b\u8d44\u683c\uff0c\u5df2\u6b63\u5f0f\u5224\u8d1f\u7ed3\u679c\u4fdd\u6301\u4e0d\u53d8",
                record.getRevokeReason(), List.of(
                        AuditFieldInput.of("eventTeamStatus", "\u8d5b\u4e8b\u7403\u961f\u72b6\u6001",
                                EventTeamStatusEnum.DISQUALIFIED.getCode(), EventTeamStatusEnum.ACTIVE.getCode())
                ));
    }

    private void disqualifyEventTeam(BasketDisciplineRecord record, BasketEventTeam eventTeam,
                                     PenaltyExecutionResultVo result) {
        if (alreadySucceeded(record.getId(), PenaltyExecutionActionTypeEnum.EVENT_TEAM_DISQUALIFIED.name(),
                TARGET_EVENT_TEAM, eventTeam.getId())) {
            result.setEventTeamStatus(EventTeamStatusEnum.DISQUALIFIED.getCode());
            return;
        }
        if (!EventTeamStatusEnum.isDisqualified(eventTeam.getEventTeamStatus())) {
            eventTeam.setEventTeamStatus(EventTeamStatusEnum.DISQUALIFIED.getCode());
            eventTeamMapper.updateById(eventTeam);
        }
        if (record.getEventTeamId() == null) {
            disciplineRecordMapper.bindEventTeamId(record.getId(), eventTeam.getId());
            record.setEventTeamId(eventTeam.getId());
        }
        result.setEventTeamStatus(EventTeamStatusEnum.DISQUALIFIED.getCode());
        saveDetail(record.getId(), PenaltyExecutionActionTypeEnum.EVENT_TEAM_DISQUALIFIED.name(),
                TARGET_EVENT_TEAM, eventTeam.getId(), DETAIL_SUCCESS, EventTeamStatusEnum.DISQUALIFIED.getCode());
        auditWriter.writeAudit("DISCIPLINE", "EVENT_TEAM", eventTeam.getId(), "EVENT_TEAM_DISQUALIFIED",
                "\u8d5b\u4e8b\u7403\u961f\u53d6\u6d88\u8d44\u683c", REASON, List.of(
                        AuditFieldInput.of("eventTeamStatus", "\u8d5b\u4e8b\u7403\u961f\u72b6\u6001",
                                eventTeam.getEventTeamStatus(), EventTeamStatusEnum.DISQUALIFIED.getCode()),
                        AuditFieldInput.of("disciplineId", "\u5904\u7f5a\u8bb0\u5f55", null, record.getId())
                ));
    }

    private void handleCompletedWinnerAdvance(BasketDisciplineRecord record, BasketEventTeam eventTeam,
                                              PenaltyExecutionResultVo result) {
        List<BasketMatchSeries> seriesList = seriesMapper.selectByDivisionId(eventTeam.getDivisionId());
        if (seriesList == null) {
            return;
        }
        for (BasketMatchSeries series : seriesList) {
            if (!MatchSeriesStatusEnum.COMPLETED.getCode().equals(series.getSeriesStatus())) {
                continue;
            }
            if (!eventTeam.getId().equals(series.getHomeEventTeamId())
                    && !eventTeam.getId().equals(series.getAwayEventTeamId())) {
                continue;
            }
            boolean dqIsWinner = eventTeam.getId().equals(series.getWinnerEventTeamId());
            List<BasketMatchSeries> next = seriesMapper.selectBySourceSeriesId(series.getId());
            boolean started = false;
            if (next != null) {
            for (BasketMatchSeries target : next) {
                if (winnerOfSeriesAdvanceSupport.isDownstreamStarted(target)) {
                    started = true;
                    break;
                }
            }
            }
            if (started && dqIsWinner) {
                if (alreadySucceeded(record.getId(), PenaltyExecutionActionTypeEnum.DOWNSTREAM_GUARD.name(),
                        TARGET_SERIES, series.getId())) {
                    result.getWarnings().add(MSG_DOWNSTREAM_STARTED_ADMIN);
                    result.setQualificationBlockedCount(result.getQualificationBlockedCount() + 1);
                    continue;
                }
                result.getWarnings().add(MSG_DOWNSTREAM_STARTED_ADMIN);
                result.setQualificationBlockedCount(result.getQualificationBlockedCount() + 1);
                saveDetail(record.getId(), PenaltyExecutionActionTypeEnum.DOWNSTREAM_GUARD.name(),
                        TARGET_SERIES, series.getId(), DETAIL_SUCCESS, MSG_DOWNSTREAM_STARTED_ADMIN);
                auditWriter.writeAudit("DISCIPLINE", "MATCH_SERIES", series.getId(), "DOWNSTREAM_GUARD",
                        MSG_DOWNSTREAM_STARTED_ADMIN, REASON, null);
                continue;
            }
            if (alreadySucceeded(record.getId(), PenaltyExecutionActionTypeEnum.SERIES_AUTO_FORFEIT.name(),
                    TARGET_SERIES, series.getId())) {
                result.setSeriesForfeitCount(result.getSeriesForfeitCount() + 1);
                result.getForfeitedSeriesIds().add(series.getId());
                continue;
            }
            BasketSeriesForfeitReq sweepReq = new BasketSeriesForfeitReq();
            sweepReq.setSeriesId(series.getId());
            sweepReq.setForfeitEventTeamId(eventTeam.getId());
            sweepReq.setReason(REASON);
            try {
                exceptionService.applySeriesOfficialSweepForDisqualification(sweepReq);
                result.setSeriesForfeitCount(result.getSeriesForfeitCount() + 1);
                result.getForfeitedSeriesIds().add(series.getId());
                saveDetail(record.getId(), PenaltyExecutionActionTypeEnum.SERIES_AUTO_FORFEIT.name(),
                        TARGET_SERIES, series.getId(), DETAIL_SUCCESS, REASON);
                auditWriter.writeAudit("DISCIPLINE", "MATCH_SERIES", series.getId(), "SERIES_AUTO_FORFEIT",
                        "\u53d6\u6d88\u8d44\u683c\u8986\u76d6\u7cfb\u5217\u8d5b\u5b98\u65b9\u7ed3\u679c", REASON, null);
                if (dqIsWinner) {
                    result.getWarnings().add(MSG_REQUIRES_ADMIN_ADJUDICATION);
                    saveDetail(record.getId(), PenaltyExecutionActionTypeEnum.REQUIRES_ADMIN_ADJUDICATION.name(),
                            TARGET_SERIES, series.getId(), DETAIL_SUCCESS, MSG_REQUIRES_ADMIN_ADJUDICATION);
                }
            } catch (ServiceException ex) {
                if (isDownstreamGuard(ex)) {
                    result.getWarnings().add(MSG_DOWNSTREAM_STARTED_ADMIN);
                    result.setQualificationBlockedCount(result.getQualificationBlockedCount() + 1);
                    saveDetail(record.getId(), PenaltyExecutionActionTypeEnum.DOWNSTREAM_GUARD.name(),
                            TARGET_SERIES, series.getId(), DETAIL_SUCCESS, MSG_DOWNSTREAM_STARTED_ADMIN);
                    auditWriter.writeAudit("DISCIPLINE", "MATCH_SERIES", series.getId(), "DOWNSTREAM_GUARD",
                            MSG_DOWNSTREAM_STARTED_ADMIN, ex.getMessage(), null);
                } else {
                    result.setErrorMessage(appendError(result.getErrorMessage(),
                            "\u7cfb\u5217\u8d5b " + series.getId() + "\uff1a" + ex.getMessage()));
                    saveDetail(record.getId(), PenaltyExecutionActionTypeEnum.SERIES_AUTO_FORFEIT.name(),
                            TARGET_SERIES, series.getId(), DETAIL_FAILED, ex.getMessage());
                }
            }
        }
    }

    private BasketEventTeam resolveEventTeam(BasketDisciplineRecord record) {
        if (record.getEventTeamId() != null) {
            BasketEventTeam team = eventTeamMapper.selectById(record.getEventTeamId());
            if (team == null) {
                throw new ServiceException("\u5904\u7f5a\u5173\u8054\u7684\u8d5b\u4e8b\u7403\u961f\u4e0d\u5b58\u5728");
            }
            return team;
        }
        if (record.getTeamId() == null || record.getSeasonId() == null) {
            throw new ServiceException("\u53d6\u6d88\u8d5b\u4e8b\u8d44\u683c\u9700\u8981\u7ed1\u5b9a\u8d5b\u4e8b\u7403\u961f\uff0c\u6216\u80fd\u552f\u4e00\u89e3\u6790\u5230\u5f53\u524d\u8d5b\u5b63 EventTeam");
        }
        List<BasketEventTeam> teams = eventTeamMapper.selectBySeasonAndSourceTeam(record.getSeasonId(), record.getTeamId());
        if (teams == null || teams.isEmpty()) {
            throw new ServiceException("\u5f53\u524d\u8d5b\u5b63\u627e\u4e0d\u5230\u8be5\u957f\u671f\u7403\u961f\u5bf9\u5e94\u7684\u8d5b\u4e8b\u7403\u961f");
        }
        if (teams.size() > 1) {
            throw new ServiceException("\u8be5\u957f\u671f\u7403\u961f\u5728\u672c\u8d5b\u5b63\u5b58\u5728\u591a\u652f\u8d5b\u4e8b\u7403\u961f\uff0c\u8bf7\u9009\u62e9\u5177\u4f53 EventTeam \u540e\u518d\u53d6\u6d88\u8d44\u683c");
        }
        return teams.get(0);
    }

    private void forfeitFutureStandaloneMatches(BasketDisciplineRecord record, BasketEventTeam eventTeam,
                                                PenaltyExecutionResultVo result) {
        List<BasketV4Match> matches = matchMapper.selectByDivisionAndEventTeam(
                eventTeam.getDivisionId(), eventTeam.getId());
        if (matches == null) {
            return;
        }
        for (BasketV4Match match : matches) {
            if (match.getSeriesId() != null) {
                continue;
            }
            if (isOfficiallySettled(match)) {
                if (MatchStatusEnum.FORFEITED.getCode().equals(match.getMatchStatus())
                        && match.getWinnerEventTeamId() != null
                        && !eventTeam.getId().equals(match.getWinnerEventTeamId())
                        && alreadySucceeded(record.getId(), PenaltyExecutionActionTypeEnum.MATCH_AUTO_FORFEIT.name(),
                        TARGET_MATCH, match.getId())) {
                    result.setFutureMatchForfeitCount(result.getFutureMatchForfeitCount() + 1);
                    result.getForfeitedMatchIds().add(match.getId());
                    continue;
                }
                if (alreadySucceeded(record.getId(), PenaltyExecutionActionTypeEnum.MATCH_AUTO_FORFEIT.name(),
                        TARGET_MATCH, match.getId())) {
                    result.setFutureMatchForfeitCount(result.getFutureMatchForfeitCount() + 1);
                    result.getForfeitedMatchIds().add(match.getId());
                    continue;
                }
                BasketMatchForfeitReq overrideReq = new BasketMatchForfeitReq();
                overrideReq.setMatchId(match.getId());
                overrideReq.setLoserEventTeamId(eventTeam.getId());
                overrideReq.setReason(REASON);
                try {
                    exceptionService.overrideMatchOfficialResultForDisqualification(overrideReq);
                    result.setFutureMatchForfeitCount(result.getFutureMatchForfeitCount() + 1);
                    result.getForfeitedMatchIds().add(match.getId());
                    saveDetail(record.getId(), PenaltyExecutionActionTypeEnum.MATCH_AUTO_FORFEIT.name(),
                            TARGET_MATCH, match.getId(), DETAIL_SUCCESS, REASON);
                    auditWriter.writeAudit("DISCIPLINE", "MATCH", match.getId(), "MATCH_AUTO_FORFEIT",
                            "\u53d6\u6d88\u8d44\u683c\u8986\u76d6\u5df2\u5b8c\u6210\u6bd4\u8d5b\u5b98\u65b9\u7ed3\u679c", REASON, null);
                } catch (ServiceException ex) {
                    result.setErrorMessage(appendError(result.getErrorMessage(),
                            "\u6bd4\u8d5b " + match.getId() + "\uff1a" + ex.getMessage()));
                    saveDetail(record.getId(), PenaltyExecutionActionTypeEnum.MATCH_AUTO_FORFEIT.name(),
                            TARGET_MATCH, match.getId(), DETAIL_FAILED, ex.getMessage());
                }
                continue;
            }
            if (!AdminOfficialResultCapability.canForfeitMatch(match)) {
                continue;
            }
            if (alreadySucceeded(record.getId(), PenaltyExecutionActionTypeEnum.MATCH_AUTO_FORFEIT.name(),
                    TARGET_MATCH, match.getId())) {
                result.setFutureMatchForfeitCount(result.getFutureMatchForfeitCount() + 1);
                result.getForfeitedMatchIds().add(match.getId());
                continue;
            }
            BasketMatchForfeitReq req = new BasketMatchForfeitReq();
            req.setMatchId(match.getId());
            req.setLoserEventTeamId(eventTeam.getId());
            req.setReason(REASON);
            try {
                exceptionService.forfeitMatch(req);
                result.setFutureMatchForfeitCount(result.getFutureMatchForfeitCount() + 1);
                result.getForfeitedMatchIds().add(match.getId());
                saveDetail(record.getId(), PenaltyExecutionActionTypeEnum.MATCH_AUTO_FORFEIT.name(),
                        TARGET_MATCH, match.getId(), DETAIL_SUCCESS, REASON);
                auditWriter.writeAudit("DISCIPLINE", "MATCH", match.getId(), "MATCH_AUTO_FORFEIT",
                        "\u53d6\u6d88\u8d44\u683c\u81ea\u52a8\u6bd4\u8d5b\u5224\u8d1f", REASON, List.of(
                                AuditFieldInput.of("loserEventTeamId", "\u5224\u8d1f\u65b9", null, eventTeam.getId())
                        ));
            } catch (ServiceException ex) {
                if (isDownstreamGuard(ex)) {
                    result.getWarnings().add(MSG_DOWNSTREAM_STARTED_ADMIN);
                    result.setQualificationBlockedCount(result.getQualificationBlockedCount() + 1);
                    saveDetail(record.getId(), PenaltyExecutionActionTypeEnum.DOWNSTREAM_GUARD.name(),
                            TARGET_MATCH, match.getId(), DETAIL_SUCCESS, MSG_DOWNSTREAM_STARTED_ADMIN);
                } else {
                    result.setErrorMessage(appendError(result.getErrorMessage(),
                            "\u6bd4\u8d5b " + match.getId() + "\uff1a" + ex.getMessage()));
                    saveDetail(record.getId(), PenaltyExecutionActionTypeEnum.MATCH_AUTO_FORFEIT.name(),
                            TARGET_MATCH, match.getId(), DETAIL_FAILED, ex.getMessage());
                }
            }
        }
    }

    private void forfeitOpenSeries(BasketDisciplineRecord record, BasketEventTeam eventTeam,
                                   PenaltyExecutionResultVo result) {
        List<BasketMatchSeries> seriesList = seriesMapper.selectByDivisionId(eventTeam.getDivisionId());
        if (seriesList == null) {
            return;
        }
        for (BasketMatchSeries series : seriesList) {
            if (!eventTeam.getId().equals(series.getHomeEventTeamId())
                    && !eventTeam.getId().equals(series.getAwayEventTeamId())) {
                continue;
            }
            if (MatchSeriesStatusEnum.COMPLETED.getCode().equals(series.getSeriesStatus())
                    || MatchSeriesStatusEnum.CANCELLED.getCode().equals(series.getSeriesStatus())) {
                continue;
            }
            if (alreadySucceeded(record.getId(), PenaltyExecutionActionTypeEnum.SERIES_AUTO_FORFEIT.name(),
                    TARGET_SERIES, series.getId())) {
                result.setSeriesForfeitCount(result.getSeriesForfeitCount() + 1);
                result.getForfeitedSeriesIds().add(series.getId());
                continue;
            }
            BasketSeriesForfeitReq req = new BasketSeriesForfeitReq();
            req.setSeriesId(series.getId());
            req.setForfeitEventTeamId(eventTeam.getId());
            req.setReason(REASON);
            try {
                exceptionService.forfeitSeries(req);
                result.setSeriesForfeitCount(result.getSeriesForfeitCount() + 1);
                result.getForfeitedSeriesIds().add(series.getId());
                saveDetail(record.getId(), PenaltyExecutionActionTypeEnum.SERIES_AUTO_FORFEIT.name(),
                        TARGET_SERIES, series.getId(), DETAIL_SUCCESS, REASON);
                auditWriter.writeAudit("DISCIPLINE", "MATCH_SERIES", series.getId(), "SERIES_AUTO_FORFEIT",
                        "\u53d6\u6d88\u8d44\u683c\u81ea\u52a8\u7cfb\u5217\u8d5b\u5224\u8d1f", REASON, List.of(
                                AuditFieldInput.of("forfeitEventTeamId", "\u5f03\u8d5b\u65b9", null, eventTeam.getId())
                        ));
            } catch (ServiceException ex) {
                if (isDownstreamGuard(ex)) {
                    result.getWarnings().add(MSG_DOWNSTREAM_STARTED_ADMIN);
                    result.setQualificationBlockedCount(result.getQualificationBlockedCount() + 1);
                    saveDetail(record.getId(), PenaltyExecutionActionTypeEnum.DOWNSTREAM_GUARD.name(),
                            TARGET_SERIES, series.getId(), DETAIL_SUCCESS, MSG_DOWNSTREAM_STARTED_ADMIN);
                    auditWriter.writeAudit("DISCIPLINE", "MATCH_SERIES", series.getId(), "QUALIFICATION_BLOCK",
                            MSG_DOWNSTREAM_STARTED_ADMIN, ex.getMessage(), null);
                } else {
                    result.setErrorMessage(appendError(result.getErrorMessage(),
                            "\u7cfb\u5217\u8d5b " + series.getId() + "\uff1a" + ex.getMessage()));
                    saveDetail(record.getId(), PenaltyExecutionActionTypeEnum.SERIES_AUTO_FORFEIT.name(),
                            TARGET_SERIES, series.getId(), DETAIL_FAILED, ex.getMessage());
                }
            }
        }
    }

    private void handleQualification(BasketDisciplineRecord record, BasketEventTeam eventTeam,
                                     PenaltyExecutionResultVo result) {
        List<BasketDivisionStage> stages = stageMapper.selectByDivisionId(eventTeam.getDivisionId());
        if (stages == null) {
            return;
        }
        for (BasketDivisionStage stage : stages) {
            List<BasketDivisionStageQualification> preview = qualificationMapper.selectPreviewByStageId(stage.getId());
            boolean removed = false;
            if (preview != null) {
                for (BasketDivisionStageQualification row : preview) {
                    if (eventTeam.getId().equals(row.getEventTeamId())) {
                        removed = true;
                        break;
                    }
                }
            }
            if (removed && !alreadySucceeded(record.getId(), PenaltyExecutionActionTypeEnum.QUALIFICATION_BLOCK.name(),
                    TARGET_STAGE, stage.getId())) {
                int n = qualificationMapper.softDeletePreviewByStageAndEventTeam(stage.getId(), eventTeam.getId());
                result.setQualificationRemovedCount(result.getQualificationRemovedCount() + n);
                saveDetail(record.getId(), PenaltyExecutionActionTypeEnum.QUALIFICATION_BLOCK.name(),
                        TARGET_STAGE, stage.getId(), DETAIL_SUCCESS, "PREVIEW_REMOVED");
                auditWriter.writeAudit("DISCIPLINE", "STAGE", stage.getId(), "QUALIFICATION_BLOCK",
                        "\u672a\u786e\u8ba4\u664b\u7ea7\u9884\u89c8\u5df2\u6392\u9664\u53d6\u6d88\u8d44\u683c\u7403\u961f", REASON, List.of(
                                AuditFieldInput.of("eventTeamId", "\u8d5b\u4e8b\u7403\u961f", null, eventTeam.getId())
                        ));
            } else if (removed) {
                result.setQualificationRemovedCount(result.getQualificationRemovedCount() + 1);
            }
            List<BasketDivisionStageQualification> confirmed = qualificationMapper.selectConfirmedByStageId(stage.getId());
            if (confirmed == null) {
                continue;
            }
            for (BasketDivisionStageQualification row : confirmed) {
                if (eventTeam.getId().equals(row.getEventTeamId())) {
                    result.getWarnings().add("\u9636\u6bb5 " + stage.getStageName()
                            + " \u664b\u7ea7\u5df2\u786e\u8ba4\uff0c\u672a\u9759\u9ed8\u91cd\u5199\u5386\u53f2\u8d44\u683c");
                    result.setQualificationBlockedCount(result.getQualificationBlockedCount() + 1);
                    if (!alreadySucceeded(record.getId(), PenaltyExecutionActionTypeEnum.QUALIFICATION_BLOCK.name(),
                            TARGET_STAGE, stage.getId())) {
                        saveDetail(record.getId(), PenaltyExecutionActionTypeEnum.QUALIFICATION_BLOCK.name(),
                                TARGET_STAGE, stage.getId(), DETAIL_SUCCESS, "CONFIRMED_BLOCK");
                        auditWriter.writeAudit("DISCIPLINE", "STAGE", stage.getId(), "QUALIFICATION_BLOCK",
                                "\u664b\u7ea7\u5df2\u786e\u8ba4\uff0c\u53d6\u6d88\u8d44\u683c\u4e0d\u81ea\u52a8\u91cd\u5199\u5386\u53f2\u8d44\u683c", REASON, null);
                    }
                    break;
                }
            }
        }
    }

    private void rebuildStanding(BasketEventTeam eventTeam, PenaltyExecutionResultVo result) {
        if (eventTeam.getDivisionId() == null) {
            return;
        }
        try {
            statsSettlementService.rebuildDivisionStats(eventTeam.getDivisionId());
        } catch (Exception ex) {
            result.getWarnings().add("\u79ef\u5206\u699c\u91cd\u5efa\uff1a" + ex.getMessage());
        }
    }

    private void summarize(PenaltyExecutionResultVo result) {
        result.setWarningCount(result.getWarnings().size());
        if (result.getErrorMessage() != null && !result.getWarnings().isEmpty()) {
            result.setExecutionStatus(PenaltyExecutionStatusEnum.PARTIAL.name());
        } else if (result.getErrorMessage() != null) {
            result.setExecutionStatus(PenaltyExecutionStatusEnum.FAILED.name());
        } else if (!result.getWarnings().isEmpty()) {
            result.setExecutionStatus(PenaltyExecutionStatusEnum.PARTIAL.name());
        } else {
            result.setExecutionStatus(PenaltyExecutionStatusEnum.EFFECTIVE.name());
        }
    }

    private boolean alreadySucceeded(Long disciplineId, String actionType, String targetType, Long targetId) {
        if (disciplineId == null || targetId == null) {
            return false;
        }
        BasketDisciplineExecutionDetail existing = executionDetailMapper.selectByAction(
                disciplineId, actionType, targetType, targetId);
        return existing != null && DETAIL_SUCCESS.equals(existing.getExecutionStatus());
    }

    private void saveDetail(Long disciplineId, String actionType, String targetType, Long targetId,
                            String status, String message) {
        if (disciplineId == null || targetId == null) {
            return;
        }
        Date now = new Date();
        BasketDisciplineExecutionDetail existing = executionDetailMapper.selectByAction(
                disciplineId, actionType, targetType, targetId);
        if (existing == null) {
            BasketDisciplineExecutionDetail row = new BasketDisciplineExecutionDetail();
            row.setDisciplineRecordId(disciplineId);
            row.setActionType(actionType);
            row.setTargetType(targetType);
            row.setTargetId(targetId);
            row.setExecutionStatus(status);
            row.setResultMessage(message);
            row.setCreateTime(now);
            row.setUpdateTime(now);
            row.setIsDelete(0);
            executionDetailMapper.insert(row);
            return;
        }
        existing.setExecutionStatus(status);
        existing.setResultMessage(message);
        executionDetailMapper.updateStatus(existing);
    }

    private boolean isOfficiallySettled(BasketV4Match match) {
        String status = match.getMatchStatus();
        return MatchStatusEnum.FINISHED.getCode().equals(status)
                || MatchStatusEnum.FORFEITED.getCode().equals(status)
                || MatchStatusEnum.CANCELLED.getCode().equals(status);
    }

    private boolean isDownstreamGuard(ServiceException ex) {
        return ex.getMessage() != null
                && ex.getMessage().contains(WinnerOfSeriesAdvanceSupport.MSG_DOWNSTREAM_STARTED);
    }

    private String appendError(String current, String next) {
        if (current == null || current.isEmpty()) {
            return next;
        }
        return current + "; " + next;
    }
}
