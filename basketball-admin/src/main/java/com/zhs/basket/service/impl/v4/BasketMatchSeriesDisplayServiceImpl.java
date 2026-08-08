package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.*;
import com.zhs.basket.enums.v4.MatchSeriesStatusEnum;
import com.zhs.basket.enums.v4.MatchStatusEnum;
import com.zhs.basket.enums.v4.QualificationRecordStatusEnum;
import com.zhs.basket.enums.v4.SubmissionStatusEnum;
import com.zhs.basket.mapper.v4.*;
import com.zhs.basket.service.v4.IBasketMatchSeriesDisplayService;
import com.zhs.basket.support.v4.MatchSeriesResultCalculator;
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

    @Override
    public BasketMatchSeriesDetailVo getDetail(Long seriesId) {
        BasketMatchSeries series = requireSeries(seriesId);
        return buildDetail(series);
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
        result.sort(Comparator
                .comparing(BasketMatchSeriesDetailVo::getRoundNo, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(BasketMatchSeriesDetailVo::getBracketSlot, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(BasketMatchSeriesDetailVo::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
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
        vo.setHomeWinCount(series.getHomeWinCount());
        vo.setAwayWinCount(series.getAwayWinCount());
        vo.setSeriesStatus(series.getSeriesStatus());
        vo.setSeriesStatusName(seriesStatusLabel(series.getSeriesStatus()));
        vo.setResolutionType(series.getResolutionType());
        vo.setResolutionReason(series.getResolutionReason());
        vo.setWinnerEventTeamId(series.getWinnerEventTeamId());

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
        fillQualification(vo, series);
        vo.setGames(buildGames(series));
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
