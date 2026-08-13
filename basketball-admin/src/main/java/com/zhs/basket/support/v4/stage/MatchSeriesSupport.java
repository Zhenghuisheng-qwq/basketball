package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketMatchSeries;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.enums.v4.MatchSeriesStatusEnum;
import com.zhs.basket.enums.v4.ParticipantSourceTypeEnum;
import com.zhs.basket.mapper.v4.BasketMatchSeriesMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.support.v4.BestOfValues;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * Shared MatchSeries + child Game factory for elimination and round-robin.
 */
@Component
public class MatchSeriesSupport {

    private static final Integer NOT_DELETED = 0;

    @Autowired
    private BasketMatchSeriesMapper seriesMapper;
    @Autowired
    private BasketV4MatchMapper matchMapper;

    public BasketMatchSeries createSeriesWithGames(StageExecutionContext context,
                                                   BasketScheduleBatch batch,
                                                   int bestOf,
                                                   int roundNo,
                                                   int slot,
                                                   Long homeEventTeamId,
                                                   Long awayEventTeamId,
                                                   Long sourceSeriesIdHome,
                                                   Long sourceSeriesIdAway,
                                                   boolean validateFirstGamePair) {
        BestOfValues.requireLegal(bestOf);
        Date now = new Date();
        BasketMatchSeries series = new BasketMatchSeries();
        BasketDivision division = context.getDivision();
        BasketDivisionStage stage = context.getStage();
        series.setDivisionId(division.getId());
        series.setStageId(stage.getId());
        series.setScheduleBatchId(batch.getId());
        series.setSeriesCode(buildSeriesCode(division.getId(), stage.getId(), roundNo, slot));
        series.setRoundNo(roundNo);
        series.setBracketSlot(slot);
        series.setHomeEventTeamId(homeEventTeamId);
        series.setAwayEventTeamId(awayEventTeamId);
        series.setBestOf(bestOf);
        series.setHomeWinCount(0);
        series.setAwayWinCount(0);
        series.setWinnerEventTeamId(null);
        series.setSeriesStatus(MatchSeriesStatusEnum.READY.getCode());
        series.setSourceSeriesIdHome(sourceSeriesIdHome);
        series.setSourceSeriesIdAway(sourceSeriesIdAway);
        if (sourceSeriesIdHome != null) {
            series.setHomeSourceType(ParticipantSourceTypeEnum.WINNER_OF_SERIES.getCode());
            series.setHomeSourceDisplay("上一轮胜者");
        }
        if (sourceSeriesIdAway != null) {
            series.setAwaySourceType(ParticipantSourceTypeEnum.WINNER_OF_SERIES.getCode());
            series.setAwaySourceDisplay("上一轮胜者");
        }
        series.setCreateTime(now);
        series.setUpdateTime(now);
        series.setIsDelete(NOT_DELETED);
        if (seriesMapper.insert(series) != 1) {
            throw new ServiceException("创建系列赛失败: " + series.getSeriesCode());
        }

        insertGames(context, batch, series, bestOf, roundNo, slot, homeEventTeamId, awayEventTeamId, validateFirstGamePair);
        return series;
    }

    public BasketMatchSeries createSeriesSkeleton(StageExecutionContext context,
                                                  BasketScheduleBatch batch,
                                                  int bestOf,
                                                  int roundNo,
                                                  int slot,
                                                  Long homeEventTeamId,
                                                  Long awayEventTeamId,
                                                  Long sourceSeriesIdHome,
                                                  Long sourceSeriesIdAway,
                                                  String homeSourceType,
                                                  String awaySourceType,
                                                  String homeSourceDisplay,
                                                  String awaySourceDisplay) {
        BestOfValues.requireLegal(bestOf);
        Date now = new Date();
        BasketMatchSeries series = new BasketMatchSeries();
        BasketDivision division = context.getDivision();
        BasketDivisionStage stage = context.getStage();
        series.setDivisionId(division.getId());
        series.setStageId(stage.getId());
        series.setScheduleBatchId(batch.getId());
        series.setSeriesCode(buildSeriesCode(division.getId(), stage.getId(), roundNo, slot));
        series.setRoundNo(roundNo);
        series.setBracketSlot(slot);
        series.setHomeEventTeamId(homeEventTeamId);
        series.setAwayEventTeamId(awayEventTeamId);
        series.setHomeSourceType(homeSourceType);
        series.setAwaySourceType(awaySourceType);
        series.setHomeSourceDisplay(homeSourceDisplay);
        series.setAwaySourceDisplay(awaySourceDisplay);
        series.setBestOf(bestOf);
        series.setHomeWinCount(0);
        series.setAwayWinCount(0);
        series.setWinnerEventTeamId(null);
        series.setSeriesStatus(MatchSeriesStatusEnum.PENDING.getCode());
        series.setSourceSeriesIdHome(sourceSeriesIdHome);
        series.setSourceSeriesIdAway(sourceSeriesIdAway);
        series.setCreateTime(now);
        series.setUpdateTime(now);
        series.setIsDelete(NOT_DELETED);
        if (seriesMapper.insert(series) != 1) {
            throw new ServiceException("创建系列赛骨架失败: " + series.getSeriesCode());
        }
        return series;
    }

    public void ensureGames(StageExecutionContext context,
                            BasketScheduleBatch batch,
                            BasketMatchSeries series,
                            Long homeEventTeamId,
                            Long awayEventTeamId) {
        if (homeEventTeamId == null || awayEventTeamId == null) {
            return;
        }
        series.setHomeEventTeamId(homeEventTeamId);
        series.setAwayEventTeamId(awayEventTeamId);
        series.setSeriesStatus(MatchSeriesStatusEnum.READY.getCode());
        series.setUpdateTime(new Date());
        seriesMapper.updateById(series);
        if (context == null || batch == null) {
            return;
        }
        List<com.zhs.basket.domain.v4.BasketV4Match> existing = matchMapper.selectBySeriesId(series.getId());
        if (existing != null && !existing.isEmpty()) {
            for (com.zhs.basket.domain.v4.BasketV4Match match : existing) {
                match.setHomeEventTeamId(homeEventTeamId);
                match.setAwayEventTeamId(awayEventTeamId);
                matchMapper.updateById(match);
            }
            return;
        }
        insertGames(context, batch, series, series.getBestOf(), series.getRoundNo(), series.getBracketSlot(),
                homeEventTeamId, awayEventTeamId, true);
    }

    public void updateSourceLabels(BasketMatchSeries series) {
        seriesMapper.updateById(series);
    }

    private void insertGames(StageExecutionContext context,
                             BasketScheduleBatch batch,
                             BasketMatchSeries series,
                             int bestOf,
                             int roundNo,
                             int slot,
                             Long homeEventTeamId,
                             Long awayEventTeamId,
                             boolean validateFirstGamePair) {
        BasketDivision division = context.getDivision();
        BasketDivisionStage stage = context.getStage();
        for (int gameNo = 1; gameNo <= bestOf; gameNo++) {
            BasketV4Match match = StageScheduleSupport.newMatch(context, batch, roundNo, slot,
                    buildSeriesGameMatchNo(division.getId(), stage.getId(), roundNo, slot, gameNo));
            match.setSeriesId(series.getId());
            match.setSeriesGameNo(gameNo);
            match.setHomeEventTeamId(homeEventTeamId);
            match.setAwayEventTeamId(awayEventTeamId);
            if (homeEventTeamId != null && awayEventTeamId != null && gameNo == 1 && validateFirstGamePair) {
                StageScheduleSupport.validateMatchPair(match, context, roundNo, slot + 1, null);
            }
            StageScheduleSupport.insertMatch(matchMapper, match);
        }
    }

    public static String buildSeriesCode(Long divisionId, Long stageId, int roundNo, int bracketSlot) {
        return String.format("V4-%d-SER-%d-R%d-S%02d", divisionId, stageId, roundNo, bracketSlot + 1);
    }

    public static String buildSeriesGameMatchNo(Long divisionId, Long stageId, int roundNo, int bracketSlot, int gameNo) {
        return String.format("V4-%d-SER-%d-R%d-S%02d-G%d", divisionId, stageId, roundNo, bracketSlot + 1, gameNo);
    }
}
