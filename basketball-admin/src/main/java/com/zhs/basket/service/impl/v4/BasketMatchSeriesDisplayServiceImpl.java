package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.*;
import com.zhs.basket.enums.v4.MatchSeriesStatusEnum;
import com.zhs.basket.enums.v4.MatchStatusEnum;
import com.zhs.basket.enums.v4.QualificationRecordStatusEnum;
import com.zhs.basket.enums.v4.SubmissionStatusEnum;
import com.zhs.basket.mapper.v4.*;
import com.zhs.basket.service.v4.IBasketMatchSeriesDisplayService;
import com.zhs.basket.support.v4.AdminOfficialResultCapability;
import com.zhs.basket.support.v4.MatchSeriesResultCalculator;
import com.zhs.basket.support.v4.schedule.ScheduleAdjustmentGuard;
import com.zhs.basket.support.v4.stage.EliminationStageSupport;
import com.zhs.basket.vo.v4.BasketMatchSeriesDetailVo;
import com.zhs.basket.vo.v4.BasketMatchSeriesGameVo;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class BasketMatchSeriesDisplayServiceImpl implements IBasketMatchSeriesDisplayService {

    @Autowired
    private BasketMatchSeriesMapper seriesMapper;
    @Autowired
    private BasketV4MatchMapper matchMapper;
    @Autowired
    private BasketDivisionStageMapper stageMapper;
    @Autowired
    private BasketEventTeamMapper eventTeamMapper;
    @Autowired
    private BasketDivisionStageQualificationMapper qualificationMapper;
    @Autowired
    private BasketMatchSubmissionMapper submissionMapper;
    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private BasketSeasonMapper seasonMapper;
    @Autowired
    private BasketScheduleBatchMapper scheduleBatchMapper;

    @Override
    public BasketMatchSeriesDetailVo getDetail(Long seriesId) {
        BasketMatchSeries series = requireSeries(seriesId);
        BasketMatchSeriesDetailVo vo = buildDetail(series);
        if (series.getStageId() != null) {
            List<BasketMatchSeries> stageList = seriesMapper.selectByStageId(series.getStageId());
            Integer maxRound = null;
            if (!CollectionUtils.isEmpty(stageList)) {
                for (BasketMatchSeries item : stageList) {
                    if (item.getRoundNo() != null) {
                        maxRound = maxRound == null ? item.getRoundNo() : Math.max(maxRound, item.getRoundNo());
                    }
                }
            }
            vo.setRoundName(knockoutRoundName(series.getRoundNo(), maxRound));
        }
        return vo;
    }

    @Override
    public List<BasketMatchSeriesDetailVo> listByStageId(Long stageId) {
        if (stageId == null) {
            throw new ServiceException("stageId 不能为空");
        }
        List<BasketMatchSeries> list = seriesMapper.selectByStageId(stageId);
        if (CollectionUtils.isEmpty(list)) {
            return List.of();
        }
        List<BasketMatchSeriesDetailVo> result = new ArrayList<>();
        for (BasketMatchSeries series : list) {
            result.add(buildDetail(series));
        }
        fillRoundNames(result);
        result.sort(Comparator
                .comparing(BasketMatchSeriesDetailVo::getRoundNo, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(BasketMatchSeriesDetailVo::getBracketSlot, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(BasketMatchSeriesDetailVo::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    @Override
    public List<BasketMatchSeriesDetailVo> listByDivisionId(Long divisionId) {
        if (divisionId == null) {
            throw new ServiceException("divisionId 不能为空");
        }
        List<BasketMatchSeries> list = seriesMapper.selectByDivisionId(divisionId);
        if (CollectionUtils.isEmpty(list)) {
            return List.of();
        }
        List<BasketMatchSeriesDetailVo> result = new ArrayList<>();
        for (BasketMatchSeries series : list) {
            result.add(buildDetail(series));
        }
        fillRoundNames(result);
        result.sort(Comparator
                .comparing(BasketMatchSeriesDetailVo::getStageId, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(BasketMatchSeriesDetailVo::getRoundNo, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(BasketMatchSeriesDetailVo::getBracketSlot, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(BasketMatchSeriesDetailVo::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    private void fillRoundNames(List<BasketMatchSeriesDetailVo> result) {
        java.util.Map<Long, Integer> maxRoundByStage = new java.util.HashMap<>();
        for (BasketMatchSeriesDetailVo vo : result) {
            if (vo.getStageId() != null && vo.getRoundNo() != null) {
                maxRoundByStage.merge(vo.getStageId(), vo.getRoundNo(), Math::max);
            }
        }
        for (BasketMatchSeriesDetailVo vo : result) {
            vo.setRoundName(knockoutRoundName(vo.getRoundNo(), maxRoundByStage.get(vo.getStageId())));
        }
    }

    @Override
    public BasketMatchSeriesDetailVo getContextByMatchId(Long matchId) {
        if (matchId == null) {
            return null;
        }
        BasketV4Match match = matchMapper.selectById(matchId);
        if (match == null || match.getSeriesId() == null) {
            return null;
        }
        BasketMatchSeries series = seriesMapper.selectById(match.getSeriesId());
        if (series == null) {
            return null;
        }
        return buildDetail(series);
    }

    private BasketMatchSeries requireSeries(Long seriesId) {
        if (seriesId == null) {
            throw new ServiceException("seriesId 不能为空");
        }
        BasketMatchSeries series = seriesMapper.selectById(seriesId);
        if (series == null) {
            throw new ServiceException("系列赛不存在");
        }
        return series;
    }

    private BasketMatchSeriesDetailVo buildDetail(BasketMatchSeries series) {
        BasketMatchSeriesDetailVo vo = new BasketMatchSeriesDetailVo();
        vo.setId(series.getId());
        vo.setSeriesCode(series.getSeriesCode());
        vo.setDivisionId(series.getDivisionId());
        vo.setStageId(series.getStageId());
        vo.setRoundNo(series.getRoundNo());
        vo.setBracketSlot(series.getBracketSlot());
        vo.setHomeEventTeamId(series.getHomeEventTeamId());
        vo.setAwayEventTeamId(series.getAwayEventTeamId());
        vo.setBestOf(series.getBestOf());
        vo.setRequiredWins(MatchSeriesResultCalculator.requiredWins(series.getBestOf()));
        int homeWins = series.getHomeWinCount() == null ? 0 : series.getHomeWinCount();
        int awayWins = series.getAwayWinCount() == null ? 0 : series.getAwayWinCount();
        boolean awardedEmpty = ("COMPLETED".equalsIgnoreCase(String.valueOf(series.getSeriesStatus()))
                || "FORFEIT".equalsIgnoreCase(String.valueOf(series.getResolutionType())))
                && homeWins == 0 && awayWins == 0 && series.getWinnerEventTeamId() != null;
        if (awardedEmpty) {
            int need = MatchSeriesResultCalculator.requiredWins(series.getBestOf() == null ? 1 : series.getBestOf());
            if (series.getWinnerEventTeamId().equals(series.getHomeEventTeamId())) {
                homeWins = need;
            } else if (series.getWinnerEventTeamId().equals(series.getAwayEventTeamId())) {
                awayWins = need;
            }
        }
        vo.setHomeWinCount(homeWins);
        vo.setAwayWinCount(awayWins);
        boolean showScore = "COMPLETED".equalsIgnoreCase(String.valueOf(series.getSeriesStatus()))
                || "FORFEIT".equalsIgnoreCase(String.valueOf(series.getResolutionType()))
                || homeWins > 0 || awayWins > 0;
        vo.setSeriesScore(showScore ? homeWins + ":" + awayWins : null);
        vo.setScheduledDate(series.getScheduledDate());
        vo.setScheduledStartTime(series.getScheduledStartTime());
        vo.setSeriesStatus(series.getSeriesStatus());
        vo.setSeriesStatusName(seriesStatusLabel(series.getSeriesStatus()));
        vo.setResolutionType(series.getResolutionType());
        vo.setResolutionReason(series.getResolutionReason());
        vo.setWinnerEventTeamId(series.getWinnerEventTeamId());

        BasketDivision division = series.getDivisionId() != null ? divisionMapper.selectById(series.getDivisionId()) : null;
        if (division != null) {
            vo.setDivisionName(division.getDivisionName());
            vo.setSeasonId(division.getSeasonId());
            if (division.getSeasonId() != null) {
                BasketSeason season = seasonMapper.selectById(division.getSeasonId());
                if (season != null) {
                    vo.setSeasonName(season.getDisplayTitle() != null ? season.getDisplayTitle() : season.getSeasonName());
                }
            }
        }
        if (series.getScheduleBatchId() != null) {
            BasketScheduleBatch batch = scheduleBatchMapper.selectById(series.getScheduleBatchId());
            if (batch != null) {
                vo.setPublishStatus(batch.getPublishStatus());
                vo.setPublishStatusName(publishStatusLabel(batch.getPublishStatus()));
            }
        }

        BasketDivisionStage stage = series.getStageId() != null ? stageMapper.selectById(series.getStageId()) : null;
        if (stage != null) {
            vo.setStageName(stage.getStageName());
            boolean finalStage = EliminationStageSupport.isFinalEliminationStage(stage);
            vo.setFinalStage(finalStage);
            if (MatchSeriesStatusEnum.COMPLETED.getCode().equals(series.getSeriesStatus())
                    && series.getWinnerEventTeamId() != null) {
                vo.setResultLabel(finalStage ? "冠军" : "本轮胜者");
            }
        }

        fillTeamNames(vo, series);
        if ((vo.getHomeTeamName() == null || vo.getHomeTeamName().isBlank())
                && series.getHomeEventTeamId() == null) {
            vo.setHomeTeamName(series.getHomeSourceDisplay() != null ? series.getHomeSourceDisplay() : "待定");
        }
        if ((vo.getAwayTeamName() == null || vo.getAwayTeamName().isBlank())
                && series.getAwayEventTeamId() == null) {
            vo.setAwayTeamName(series.getAwaySourceDisplay() != null ? series.getAwaySourceDisplay() : "待定");
        }
        fillQualification(vo, series);
        vo.setGames(buildGames(series));
        int gamesPlayed = 0;
        if (vo.getGames() != null) {
            for (BasketMatchSeriesGameVo game : vo.getGames()) {
                if (game != null && !Boolean.TRUE.equals(game.getSkipped())
                        && (MatchStatusEnum.FINISHED.getCode().equals(game.getMatchStatus())
                        || MatchStatusEnum.FORFEITED.getCode().equals(game.getMatchStatus()))) {
                    gamesPlayed++;
                }
            }
        }
        vo.setGamesPlayed(gamesPlayed);
        vo.setRoundName(knockoutRoundName(series.getRoundNo(), series.getRoundNo()));
        boolean childStarted = false;
        if (vo.getGames() != null) {
            for (BasketMatchSeriesGameVo game : vo.getGames()) {
                if (game == null || Boolean.TRUE.equals(game.getSkipped())) {
                    continue;
                }
                if (ScheduleAdjustmentGuard.isMatchStarted(game.getMatchStatus())
                        || ScheduleAdjustmentGuard.isMatchFinished(game.getMatchStatus())) {
                    childStarted = true;
                    break;
                }
            }
        }
        String block = ScheduleAdjustmentGuard.matchManagementBlockReason(
                vo.getPublishStatus(), vo.getSeriesStatus(), childStarted);
        vo.setAdjustScheduleBlockReason(block);
        vo.setCanAdjustSchedule(block == null);
        vo.setCanResolveSeriesManually(AdminOfficialResultCapability.canResolveSeriesManually(series));
        return vo;
    }

    private void fillTeamNames(BasketMatchSeriesDetailVo vo, BasketMatchSeries series) {
        if (series.getHomeEventTeamId() != null) {
            BasketEventTeam home = eventTeamMapper.selectById(series.getHomeEventTeamId());
            if (home != null) {
                vo.setHomeTeamName(home.getTeamNameSnapshot());
                vo.setHomeTeamLogoFileId(home.getTeamLogoFileId());
            }
        }
        if (series.getAwayEventTeamId() != null) {
            BasketEventTeam away = eventTeamMapper.selectById(series.getAwayEventTeamId());
            if (away != null) {
                vo.setAwayTeamName(away.getTeamNameSnapshot());
                vo.setAwayTeamLogoFileId(away.getTeamLogoFileId());
            }
        }
        if (series.getWinnerEventTeamId() != null) {
            if (series.getWinnerEventTeamId().equals(series.getHomeEventTeamId())) {
                vo.setWinnerTeamName(vo.getHomeTeamName());
            } else if (series.getWinnerEventTeamId().equals(series.getAwayEventTeamId())) {
                vo.setWinnerTeamName(vo.getAwayTeamName());
            } else {
                BasketEventTeam winner = eventTeamMapper.selectById(series.getWinnerEventTeamId());
                if (winner != null) {
                    vo.setWinnerTeamName(winner.getTeamNameSnapshot());
                }
            }
        }
    }

    private void fillQualification(BasketMatchSeriesDetailVo vo, BasketMatchSeries series) {
        if (Boolean.TRUE.equals(vo.getFinalStage())) {
            return;
        }
        BasketDivisionStageQualification qual = qualificationMapper.selectActiveBySourceSeriesId(series.getId());
        if (qual == null) {
            return;
        }
        vo.setQualificationStatus(qual.getQualificationStatus());
        vo.setQualificationStatusName(qualificationStatusLabel(qual.getQualificationStatus()));
    }

    private List<BasketMatchSeriesGameVo> buildGames(BasketMatchSeries series) {
        List<BasketV4Match> matches = matchMapper.selectBySeriesId(series.getId());
        if (CollectionUtils.isEmpty(matches)) {
            return List.of();
        }
        matches.sort(Comparator.comparing(BasketV4Match::getSeriesGameNo, Comparator.nullsLast(Comparator.naturalOrder())));
        boolean seriesCompleted = MatchSeriesStatusEnum.COMPLETED.getCode().equals(series.getSeriesStatus());
        List<BasketMatchSeriesGameVo> games = new ArrayList<>();
        for (BasketV4Match match : matches) {
            BasketMatchSeriesGameVo game = new BasketMatchSeriesGameVo();
            game.setMatchId(match.getId());
            game.setMatchCode(match.getMatchNo());
            game.setSeriesGameNo(match.getSeriesGameNo());
            game.setMatchStatus(match.getMatchStatus());
            game.setMatchStatusName(matchStatusLabel(match.getMatchStatus()));
            game.setHomeScore(match.getHomeScore());
            game.setAwayScore(match.getAwayScore());
            game.setWinnerEventTeamId(match.getWinnerEventTeamId());
            game.setScheduledTime(match.getMatchTime());
            fillSubmissionSummary(game, match);
            if (seriesCompleted && !isOfficialResult(match)) {
                game.setSkipped(true);
                game.setSkippedReason("系列赛已提前结束，本局无需进行");
            }
            games.add(game);
        }
        return games;
    }

    private void fillSubmissionSummary(BasketMatchSeriesGameVo game, BasketV4Match match) {
        if (isOfficialResult(match)) {
            game.setSubmissionStatus(SubmissionStatusEnum.APPROVED.getCode());
            game.setSubmissionStatusName(submissionStatusLabel(SubmissionStatusEnum.APPROVED.getCode()));
            game.setAuditStatus("APPROVED");
            game.setAuditStatusName("已审核");
            return;
        }
        BasketMatchSubmission homeSub = submissionMapper.selectLatestByMatchAndTeam(match.getId(), match.getHomeEventTeamId());
        BasketMatchSubmission awaySub = submissionMapper.selectLatestByMatchAndTeam(match.getId(), match.getAwayEventTeamId());
        BasketMatchSubmission primary = pickPrimarySubmission(homeSub, awaySub);
        if (primary == null) {
            game.setSubmissionStatus("NONE");
            game.setSubmissionStatusName("未提交");
            game.setAuditStatus("NONE");
            game.setAuditStatusName("未审核");
            return;
        }
        game.setSubmissionStatus(primary.getSubmissionStatus());
        game.setSubmissionStatusName(submissionStatusLabel(primary.getSubmissionStatus()));
        game.setAuditStatus(mapAuditStatus(primary.getSubmissionStatus()));
        game.setAuditStatusName(auditStatusLabel(game.getAuditStatus()));
    }

    private BasketMatchSubmission pickPrimarySubmission(BasketMatchSubmission home, BasketMatchSubmission away) {
        if (home == null) {
            return away;
        }
        if (away == null) {
            return home;
        }
        if (SubmissionStatusEnum.PENDING_REVIEW.getCode().equals(home.getSubmissionStatus())
                || SubmissionStatusEnum.PENDING_REVIEW.getCode().equals(away.getSubmissionStatus())) {
            return SubmissionStatusEnum.PENDING_REVIEW.getCode().equals(home.getSubmissionStatus()) ? home : away;
        }
        return home.getVersionNo() >= away.getVersionNo() ? home : away;
    }

    private boolean isOfficialResult(BasketV4Match match) {
        return MatchStatusEnum.FINISHED.getCode().equals(match.getMatchStatus())
                || MatchStatusEnum.FORFEITED.getCode().equals(match.getMatchStatus());
    }

    private String seriesStatusLabel(String status) {
        if (status == null) {
            return "--";
        }
        try {
            MatchSeriesStatusEnum e = MatchSeriesStatusEnum.fromCode(status);
            return switch (e) {
                case PENDING -> "待准备";
                case READY -> "待比赛";
                case RUNNING -> "进行中";
                case COMPLETED -> "已结束";
                case CANCELLED -> "已取消";
            };
        } catch (IllegalArgumentException ex) {
            return status;
        }
    }

    private String matchStatusLabel(String status) {
        if (status == null) {
            return "--";
        }
        try {
            return MatchStatusEnum.fromCode(status).getDescription();
        } catch (Exception ex) {
            return status;
        }
    }

    private String submissionStatusLabel(String status) {
        if (status == null || "NONE".equals(status)) {
            return "未提交";
        }
        try {
            return SubmissionStatusEnum.fromCode(status).getDescription();
        } catch (Exception ex) {
            return status;
        }
    }

    private String mapAuditStatus(String submissionStatus) {
        if (SubmissionStatusEnum.APPROVED.getCode().equals(submissionStatus)) {
            return "APPROVED";
        }
        if (SubmissionStatusEnum.PENDING_REVIEW.getCode().equals(submissionStatus)) {
            return "PENDING";
        }
        if (SubmissionStatusEnum.REJECTED.getCode().equals(submissionStatus)) {
            return "REJECTED";
        }
        return "NONE";
    }

    private String auditStatusLabel(String auditStatus) {
        return switch (auditStatus) {
            case "APPROVED" -> "已审核";
            case "PENDING" -> "待审核";
            case "REJECTED" -> "已驳回";
            default -> "未审核";
        };
    }

    private String knockoutRoundName(Integer roundNo, Integer maxRound) {
        if (roundNo == null) {
            return "--";
        }
        if (maxRound == null || maxRound < 1 || roundNo < 1) {
            return "R" + roundNo;
        }
        int shift = maxRound - roundNo + 1;
        if (shift < 1 || shift > 8) {
            return "R" + roundNo;
        }
        int remaining = 1 << shift;
        if (remaining <= 2) {
            return "Final";
        }
        if (remaining == 4) {
            return "SF";
        }
        return "R" + remaining;
    }

    private String publishStatusLabel(String status) {
        if (status == null) {
            return "--";
        }
        return switch (status) {
            case "PUBLISHED" -> "已发布";
            case "DRAFT" -> "草稿";
            case "WITHDRAWN" -> "已撤回";
            default -> status;
        };
    }

    private String qualificationStatusLabel(String status) {
        if (QualificationRecordStatusEnum.PREVIEW.getCode().equals(status)) {
            return "待确认晋级";
        }
        if (QualificationRecordStatusEnum.CONFIRMED.getCode().equals(status)) {
            return "已确认晋级";
        }
        return status != null ? status : "--";
    }
}
