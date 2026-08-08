package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketDivisionStageQualification;
import com.zhs.basket.domain.v4.BasketMatchSeries;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.enums.v4.AdvancementTypeEnum;
import com.zhs.basket.enums.v4.MatchSeriesStatusEnum;
import com.zhs.basket.enums.v4.MatchStatusEnum;
import com.zhs.basket.mapper.v4.BasketDivisionStageMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageQualificationMapper;
import com.zhs.basket.mapper.v4.BasketMatchSeriesMapper;
import com.zhs.basket.mapper.v4.BasketScheduleBatchMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.service.v4.IDivisionStageExecutionApplicationService;
import com.zhs.basket.support.v4.MatchSeriesQualificationSyncService;
import com.zhs.basket.support.v4.MatchSeriesResultSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasketMatchSeriesServiceImplR23Test {

    @Mock
    private BasketMatchSeriesMapper seriesMapper;
    @Mock
    private BasketV4MatchMapper matchMapper;
    @Mock
    private MatchSeriesQualificationSyncService qualificationSyncService;
    @Mock
    private IDivisionStageExecutionApplicationService stageExecutionService;

    @InjectMocks
    private BasketMatchSeriesServiceImpl service;

    @Mock
    private BasketDivisionStageMapper stageMapper;
    @Mock
    private BasketDivisionStageQualificationMapper qualificationMapper;
    @Mock
    private BasketScheduleBatchMapper scheduleBatchMapper;

    @InjectMocks
    private MatchSeriesQualificationSyncService syncService;

    @Test
    void rebuild_bo3_game1_homeWin_running() {
        BasketMatchSeries series = baseSeries();
        when(seriesMapper.selectByIdForUpdate(1L)).thenReturn(series);
        when(seriesMapper.selectById(1L)).thenReturn(series);
        when(matchMapper.selectEffectiveBySeriesId(1L)).thenReturn(List.of(finishedGame(1, 10L)));
        when(seriesMapper.updateResult(any())).thenReturn(1);

        service.rebuildSeriesResult(1L);

        verify(qualificationSyncService).syncAfterSeriesRebuild(any(), any(), any(), any());
    }

    @Test
    void rebuild_bo3_2_0_completed() {
        BasketMatchSeries series = baseSeries();
        when(seriesMapper.selectByIdForUpdate(1L)).thenReturn(series);
        when(seriesMapper.selectById(1L)).thenReturn(series);
        when(matchMapper.selectEffectiveBySeriesId(1L)).thenReturn(List.of(
                finishedGame(1, 10L),
                finishedGame(2, 10L)));
        when(seriesMapper.updateResult(any())).thenReturn(1);

        service.rebuildSeriesResult(1L);

        verify(stageExecutionService).checkStageCompletion(20L);
    }

    @Test
    void sync_completed_nonFinal_insertsPreviewQualification() {
        BasketMatchSeries series = baseSeries();
        series.setStageId(20L);
        series.setSeriesStatus(MatchSeriesStatusEnum.COMPLETED.getCode());

        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setId(20L);
        stage.setDivisionId(100L);
        stage.setAdvancementType(AdvancementTypeEnum.WINNER_ADVANCE.getCode());
        stage.setNextStageCode("SF");

        when(stageMapper.selectById(20L)).thenReturn(stage);
        when(stageMapper.selectByDivisionId(100L)).thenReturn(List.of(stage));
        when(qualificationMapper.selectActiveBySourceSeriesId(1L)).thenReturn(null);
        when(qualificationMapper.insert(any())).thenReturn(1);

        syncService.syncAfterSeriesRebuild(series, MatchSeriesStatusEnum.RUNNING.getCode(), null,
                snapshot(2, 0, 10L, true));

        verify(qualificationMapper).insert(any(BasketDivisionStageQualification.class));
    }

    @Test
    void sync_rollback_removesQualification() {
        BasketMatchSeries series = baseSeries();
        series.setStageId(20L);

        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setId(20L);
        stage.setDivisionId(100L);
        stage.setAdvancementType(AdvancementTypeEnum.WINNER_ADVANCE.getCode());
        stage.setNextStageCode("SF");

        BasketDivisionStageQualification existing = new BasketDivisionStageQualification();
        existing.setId(99L);

        when(stageMapper.selectById(20L)).thenReturn(stage);
        when(stageMapper.selectByDivisionId(100L)).thenReturn(List.of(stage));
        when(qualificationMapper.selectActiveBySourceSeriesId(1L)).thenReturn(existing);

        syncService.syncAfterSeriesRebuild(series, MatchSeriesStatusEnum.COMPLETED.getCode(), 10L,
                snapshot(1, 1, null, false));

        verify(qualificationMapper).softDeleteActiveBySourceSeriesId(1L);
        verify(qualificationMapper, never()).insert(any());
    }

    private BasketMatchSeries baseSeries() {
        BasketMatchSeries series = new BasketMatchSeries();
        series.setId(1L);
        series.setBestOf(3);
        series.setHomeEventTeamId(10L);
        series.setAwayEventTeamId(20L);
        series.setStageId(20L);
        series.setBracketSlot(0);
        series.setSeriesStatus(MatchSeriesStatusEnum.READY.getCode());
        return series;
    }

    private BasketV4Match finishedGame(int gameNo, Long winner) {
        BasketV4Match match = new BasketV4Match();
        match.setId((long) gameNo);
        match.setSeriesId(1L);
        match.setSeriesGameNo(gameNo);
        match.setHomeEventTeamId(10L);
        match.setAwayEventTeamId(20L);
        match.setWinnerEventTeamId(winner);
        match.setMatchStatus(MatchStatusEnum.FINISHED.getCode());
        return match;
    }

    private MatchSeriesResultSnapshot snapshot(int home, int away, Long winner, boolean completed) {
        MatchSeriesResultSnapshot s = new MatchSeriesResultSnapshot();
        s.setHomeWinCount(home);
        s.setAwayWinCount(away);
        s.setWinnerEventTeamId(winner);
        s.setCompleted(completed);
        s.setRequiredWins(2);
        return s;
    }
}
