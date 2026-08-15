package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketDivisionStageQualification;
import com.zhs.basket.domain.v4.BasketMatchSeries;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.enums.v4.MatchSeriesStatusEnum;
import com.zhs.basket.enums.v4.MatchStatusEnum;
import com.zhs.basket.enums.v4.QualificationRecordStatusEnum;
import com.zhs.basket.enums.v4.QualificationTypeEnum;
import com.zhs.basket.mapper.v4.BasketDivisionStageMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageQualificationMapper;
import com.zhs.basket.mapper.v4.BasketMatchSeriesMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.support.v4.schedule.WinnerOfSeriesAdvanceSupport;
import com.zhs.basket.support.v4.stage.EliminationStageSupport;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * Series 完成/回退时同步 PREVIEW 晋级记录（沿用 BO1 确认机制，不自动 CONFIRMED）。
 */
@Component
public class MatchSeriesQualificationSyncService {

    private static final int NOT_DELETED = 0;

    @Autowired
    private BasketDivisionStageMapper stageMapper;
    @Autowired
    private BasketDivisionStageQualificationMapper qualificationMapper;
    @Autowired
    private BasketV4MatchMapper matchMapper;
    @Autowired
    private BasketMatchSeriesMapper seriesMapper;

    public void syncAfterSeriesRebuild(BasketMatchSeries series,
                                       String previousStatus,
                                       Long previousWinner,
                                       MatchSeriesResultSnapshot snapshot) {
        if (series == null || series.getStageId() == null) {
            return;
        }
        if (MatchSeriesStatusEnum.CANCELLED.getCode().equals(series.getSeriesStatus())) {
            return;
        }

        BasketDivisionStage stage = stageMapper.selectById(series.getStageId());
        if (stage == null) {
            return;
        }

        boolean nonFinalWinnerAdvance = EliminationStageSupport.isWinnerAdvanceNonFinalStage(stage);
        boolean isFinal = EliminationStageSupport.isFinalEliminationStage(stage);

        BasketDivisionStageQualification existing = qualificationMapper.selectActiveBySourceSeriesId(series.getId());
        boolean wasCompleted = MatchSeriesStatusEnum.COMPLETED.getCode().equals(previousStatus);
        boolean nowCompleted = snapshot.isCompleted();

        if (nowCompleted) {
            Long winner = snapshot.getWinnerEventTeamId();
            if (winner == null || isFinal || !nonFinalWinnerAdvance) {
                return;
            }
            if (existing != null && winner.equals(existing.getEventTeamId())) {
                return;
            }
            if (existing != null && !winner.equals(existing.getEventTeamId())) {
                assertCanModifyDownstream(stage);
                qualificationMapper.softDeleteActiveBySourceSeriesId(series.getId());
            }
            upsertPreviewQualification(series, stage, winner);
            return;
        }

        if ((wasCompleted || existing != null) && existing != null) {
            assertCanModifyDownstream(stage);
            qualificationMapper.softDeleteActiveBySourceSeriesId(series.getId());
        }
    }

    private void upsertPreviewQualification(BasketMatchSeries series, BasketDivisionStage stage, Long winnerEventTeamId) {
        BasketDivisionStageQualification row = new BasketDivisionStageQualification();
        row.setDivisionId(stage.getDivisionId());
        row.setStageId(stage.getId());
        row.setEventTeamId(winnerEventTeamId);
        row.setQualificationType(QualificationTypeEnum.SERIES_WINNER.getCode());
        row.setSourceRank(series.getBracketSlot() != null ? series.getBracketSlot() + 1 : null);
        row.setTargetStageId(resolveTargetStageId(stage));
        row.setSourceSeriesId(series.getId());
        row.setQualificationStatus(QualificationRecordStatusEnum.PREVIEW.getCode());
        row.setActiveUniqueKey(buildSeriesQualificationKey(series.getId(), QualificationRecordStatusEnum.PREVIEW.getCode()));
        Date now = new Date();
        row.setCreateTime(now);
        row.setUpdateTime(now);
        row.setIsDelete(NOT_DELETED);
        if (qualificationMapper.insert(row) != 1) {
            throw new ServiceException("写入系列赛晋级预览失败");
        }
    }

    private Long resolveTargetStageId(BasketDivisionStage stage) {
        if (!StringUtils.hasText(stage.getNextStageCode())) {
            return null;
        }
        return stageMapper.selectByDivisionId(stage.getDivisionId()).stream()
                .filter(s -> stage.getNextStageCode().equals(s.getStageCode()))
                .map(BasketDivisionStage::getId)
                .findFirst()
                .orElse(null);
    }

    public void assertCanModifyDownstream(BasketDivisionStage stage) {
        if (hasOfficialDownstreamStarted(stage)) {
            throw new ServiceException(WinnerOfSeriesAdvanceSupport.MSG_DOWNSTREAM_STARTED);
        }
    }

    public boolean hasOfficialDownstreamStarted(BasketDivisionStage stage) {
        if (stage == null || !StringUtils.hasText(stage.getNextStageCode())) {
            return false;
        }
        BasketDivisionStage nextStage = stageMapper.selectByDivisionId(stage.getDivisionId()).stream()
                .filter(s -> stage.getNextStageCode().equals(s.getStageCode()))
                .findFirst()
                .orElse(null);
        return nextStage != null && hasOfficialExecution(nextStage);
    }

    private boolean hasOfficialExecution(BasketDivisionStage nextStage) {
        List<BasketV4Match> matches = matchMapper.selectByStageId(nextStage.getId());
        if (matches != null) {
            for (BasketV4Match match : matches) {
                String status = match.getMatchStatus();
                if (MatchStatusEnum.PLAYING.getCode().equals(status)
                        || MatchStatusEnum.FINISHED.getCode().equals(status)
                        || MatchStatusEnum.FORFEITED.getCode().equals(status)) {
                    return true;
                }
            }
        }
        List<BasketMatchSeries> seriesList = seriesMapper.selectByStageId(nextStage.getId());
        if (seriesList != null) {
            for (BasketMatchSeries series : seriesList) {
                if (MatchSeriesStatusEnum.RUNNING.getCode().equals(series.getSeriesStatus())
                        || MatchSeriesStatusEnum.COMPLETED.getCode().equals(series.getSeriesStatus())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String buildSeriesQualificationKey(Long seriesId, String status) {
        return "SERIES:" + seriesId + ":" + status;
    }
}
