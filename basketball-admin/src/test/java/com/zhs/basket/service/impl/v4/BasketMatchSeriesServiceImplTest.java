package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketMatchSeries;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.enums.v4.MatchSeriesStatusEnum;
import com.zhs.basket.enums.v4.MatchStatusEnum;
import com.zhs.basket.mapper.v4.BasketMatchSeriesMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.service.v4.IBasketMatchSeriesService;
import com.zhs.basket.service.v4.IDivisionStageExecutionApplicationService;
import com.zhs.basket.support.v4.MatchSeriesQualificationSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasketMatchSeriesServiceImplTest {

    @Mock
    private BasketMatchSeriesMapper seriesMapper;
    @Mock
    private BasketV4MatchMapper matchMapper;
    @Mock
    private MatchSeriesQualificationSyncService qualificationSyncService;
    @Mock
    private IDivisionStageExecutionApplicationService stageExecutionService;
    @Mock
    private com.zhs.basket.support.v4.schedule.WinnerOfSeriesAdvanceSupport winnerOfSeriesAdvanceSupport;
    @Mock
    private com.zhs.basket.support.v4.V4PublicReadCachePublisher publicReadCachePublisher;

    @InjectMocks
    private BasketMatchSeriesServiceImpl service;

    @Test
    void rebuildSeriesResult_bo3_2_0() {
        BasketMatchSeries series = new BasketMatchSeries();
        series.setId(1L);
        series.setBestOf(3);
        series.setHomeEventTeamId(10L);
        series.setAwayEventTeamId(20L);
        series.setStageId(20L);
        series.setSeriesStatus(MatchSeriesStatusEnum.RUNNING.getCode());

        BasketV4Match g1 = match(1L, 1, 10L);
        BasketV4Match g2 = match(2L, 2, 10L);

        when(seriesMapper.selectByIdForUpdate(1L)).thenReturn(series);
        when(seriesMapper.selectById(1L)).thenReturn(series);
        when(matchMapper.selectEffectiveBySeriesId(1L)).thenReturn(List.of(g1, g2));
        when(seriesMapper.updateResult(any())).thenReturn(1);

        service.rebuildSeriesResult(1L);

        verify(seriesMapper).updateResult(any(BasketMatchSeries.class));
    }

    @Test
    void listByStageId_returnsMapperResult() {
        BasketMatchSeries series = new BasketMatchSeries();
        series.setId(5L);
        when(seriesMapper.selectByStageId(99L)).thenReturn(List.of(series));

        List<BasketMatchSeries> result = service.listByStageId(99L);
        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).getId());
    }

    private BasketV4Match match(Long id, int gameNo, Long winner) {
        BasketV4Match m = new BasketV4Match();
        m.setId(id);
        m.setSeriesId(1L);
        m.setSeriesGameNo(gameNo);
        m.setHomeEventTeamId(10L);
        m.setAwayEventTeamId(20L);
        m.setWinnerEventTeamId(winner);
        m.setMatchStatus(MatchStatusEnum.FINISHED.getCode());
        return m;
    }
}
