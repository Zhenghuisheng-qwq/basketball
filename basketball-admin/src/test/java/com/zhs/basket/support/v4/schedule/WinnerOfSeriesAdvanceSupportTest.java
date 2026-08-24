package com.zhs.basket.support.v4.schedule;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketMatchSeries;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.enums.v4.MatchSeriesStatusEnum;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageMapper;
import com.zhs.basket.mapper.v4.BasketMatchSeriesMapper;
import com.zhs.basket.mapper.v4.BasketScheduleBatchMapper;
import com.zhs.basket.support.v4.stage.MatchSeriesSupport;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WinnerOfSeriesAdvanceSupportTest {

    @Mock private BasketMatchSeriesMapper matchSeriesMapper;
    @Mock private BasketScheduleBatchMapper scheduleBatchMapper;
    @Mock private BasketDivisionMapper divisionMapper;
    @Mock private BasketDivisionStageMapper stageMapper;
    @Mock private MatchSeriesSupport matchSeriesSupport;
    @Mock private com.zhs.basket.mapper.v4.BasketV4MatchMapper matchMapper;
    @Mock private com.zhs.basket.support.v4.EventTeamParticipationGuard eventTeamParticipationGuard;
    @InjectMocks private WinnerOfSeriesAdvanceSupport support;

    @Test
    void incompleteSeries_doesNotAdvanceOnSingleGameWinner() {
        BasketMatchSeries qf = new BasketMatchSeries();
        qf.setId(11L);
        qf.setWinnerEventTeamId(null);
        qf.setSeriesStatus(MatchSeriesStatusEnum.RUNNING.getCode());
        when(matchSeriesMapper.selectBySourceSeriesId(11L)).thenReturn(List.of());
        support.advanceIfCompleted(qf);
        verify(matchSeriesMapper, never()).updatePairing(any());
    }

    @Test
    void firstResolve_fillsNextRound_keepsSchedule() {
        BasketMatchSeries qf = completed(11L, 100L);
        BasketMatchSeries sf = nextRound(21L, 11L, null);
        sf.setScheduledDate(LocalDate.of(2026, 11, 3));
        sf.setScheduledStartTime(LocalTime.of(21, 0));
        when(matchSeriesMapper.selectBySourceSeriesId(11L)).thenReturn(List.of(sf));
        when(matchSeriesMapper.updatePairing(sf)).thenReturn(1);

        support.advanceIfCompleted(qf);

        assertEquals(100L, sf.getHomeEventTeamId());
        assertEquals(LocalDate.of(2026, 11, 3), sf.getScheduledDate());
        assertEquals(LocalTime.of(21, 0), sf.getScheduledStartTime());
        verify(matchSeriesSupport, never()).ensureGames(any(), any(), any(), any(), any());
    }

    @Test
    void rebindDifferentTeam_allowedWhenDownstreamNotStarted() {
        BasketMatchSeries qf = completed(11L, 100L);
        BasketMatchSeries sf = nextRound(21L, 11L, null);
        sf.setHomeEventTeamId(999L);
        sf.setSeriesStatus(MatchSeriesStatusEnum.READY.getCode());
        when(matchSeriesMapper.selectBySourceSeriesId(11L)).thenReturn(List.of(sf));
        when(matchSeriesMapper.updatePairing(sf)).thenReturn(1);
        when(matchMapper.selectBySeriesId(21L)).thenReturn(List.of());

        support.advanceIfCompleted(qf);

        assertEquals(100L, sf.getHomeEventTeamId());
        verify(matchSeriesMapper).updatePairing(sf);
    }

    @Test
    void rebindDifferentTeam_rejectedWhenDownstreamHasOfficialResult() {
        BasketMatchSeries qf = completed(11L, 100L);
        BasketMatchSeries sf = nextRound(21L, 11L, null);
        sf.setHomeEventTeamId(999L);
        sf.setSeriesStatus(MatchSeriesStatusEnum.READY.getCode());
        com.zhs.basket.domain.v4.BasketV4Match finished = new com.zhs.basket.domain.v4.BasketV4Match();
        finished.setMatchStatus(com.zhs.basket.enums.v4.MatchStatusEnum.FINISHED.getCode());
        when(matchSeriesMapper.selectBySourceSeriesId(11L)).thenReturn(List.of(sf));
        when(matchMapper.selectBySeriesId(21L)).thenReturn(List.of(finished));
        assertThrows(ServiceException.class, () -> support.advanceIfCompleted(qf));
        verify(matchSeriesMapper, never()).updatePairing(any());
    }

    @Test
    void bothSidesResolved_ensureGamesCalledWithoutClearingSchedule() {
        BasketMatchSeries qf = completed(11L, 100L);
        BasketMatchSeries sf = nextRound(21L, 11L, 12L);
        sf.setAwayEventTeamId(200L);
        sf.setScheduledDate(LocalDate.of(2026, 11, 3));
        sf.setScheduledStartTime(LocalTime.of(21, 0));
        when(matchSeriesMapper.selectBySourceSeriesId(11L)).thenReturn(List.of(sf));
        when(matchSeriesMapper.updatePairing(sf)).thenReturn(1);
        when(divisionMapper.selectById(1L)).thenReturn(new BasketDivision());
        when(stageMapper.selectById(2L)).thenReturn(new BasketDivisionStage());
        when(scheduleBatchMapper.selectById(3L)).thenReturn(new BasketScheduleBatch());

        support.advanceIfCompleted(qf);

        verify(matchSeriesSupport).ensureGames(any(), any(), eq(sf), eq(100L), eq(200L));
        assertEquals(LocalDate.of(2026, 11, 3), sf.getScheduledDate());
    }

    private BasketMatchSeries completed(Long id, Long winner) {
        BasketMatchSeries s = new BasketMatchSeries();
        s.setId(id);
        s.setWinnerEventTeamId(winner);
        s.setSeriesStatus(MatchSeriesStatusEnum.COMPLETED.getCode());
        return s;
    }

    private BasketMatchSeries nextRound(Long id, Long homeSrc, Long awaySrc) {
        BasketMatchSeries s = new BasketMatchSeries();
        s.setId(id);
        s.setDivisionId(1L);
        s.setStageId(2L);
        s.setScheduleBatchId(3L);
        s.setSourceSeriesIdHome(homeSrc);
        s.setSourceSeriesIdAway(awaySrc);
        s.setHomeEventTeamId(0L);
        s.setAwayEventTeamId(0L);
        return s;
    }
}
