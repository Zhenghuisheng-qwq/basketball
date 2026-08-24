package com.zhs.basket.support.v4.schedule;

import com.zhs.basket.domain.v4.BasketMatchSeries;
import com.zhs.basket.enums.v4.MatchSeriesStatusEnum;
import com.zhs.basket.enums.v4.ScheduleStatusEnum;
import com.zhs.basket.mapper.v4.BasketMatchSeriesMapper;
import com.zhs.basket.support.v4.stage.MatchSeriesSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WinnerAdvanceSchedulePreservationTest {

    @Mock private BasketMatchSeriesMapper matchSeriesMapper;
    @Mock private com.zhs.basket.mapper.v4.BasketScheduleBatchMapper scheduleBatchMapper;
    @Mock private com.zhs.basket.mapper.v4.BasketDivisionMapper divisionMapper;
    @Mock private com.zhs.basket.mapper.v4.BasketDivisionStageMapper stageMapper;
    @Mock private MatchSeriesSupport matchSeriesSupport;
    @Mock private com.zhs.basket.mapper.v4.BasketV4MatchMapper matchMapper;
    @Mock private com.zhs.basket.support.v4.EventTeamParticipationGuard eventTeamParticipationGuard;
    @InjectMocks private WinnerOfSeriesAdvanceSupport support;

    @Test
    void resolveHome_keepsSeriesIdAndSchedule() {
        BasketMatchSeries qf = completed(62L, 296L);
        BasketMatchSeries sf = placeholder(66L, 62L, 63L);
        when(matchSeriesMapper.selectBySourceSeriesId(62L)).thenReturn(List.of(sf));
        when(matchSeriesMapper.updatePairing(sf)).thenReturn(1);

        support.advanceIfCompleted(qf);

        assertEquals(66L, sf.getId());
        assertEquals(296L, sf.getHomeEventTeamId());
        assertNull(positive(sf.getAwayEventTeamId()));
        assertEquals(LocalDate.of(2026, 9, 6), sf.getScheduledDate());
        assertEquals(LocalTime.of(21, 0), sf.getScheduledStartTime());
        assertEquals(ScheduleStatusEnum.SCHEDULED.getCode(), sf.getScheduleStatus());
        verify(matchSeriesMapper).updatePairing(sf);
        verify(matchSeriesMapper, never()).insert(any());
        verify(matchSeriesSupport, never()).ensureGames(any(), any(), any(), any(), any());
    }

    @Test
    void resolveBothSides_stillKeepsSchedule() {
        BasketMatchSeries qf = completed(63L, 299L);
        BasketMatchSeries sf = placeholder(66L, 62L, 63L);
        sf.setHomeEventTeamId(296L);
        when(matchSeriesMapper.selectBySourceSeriesId(63L)).thenReturn(List.of(sf));
        when(matchSeriesMapper.updatePairing(sf)).thenReturn(1);
        when(divisionMapper.selectById(177L)).thenReturn(new com.zhs.basket.domain.v4.BasketDivision());
        when(stageMapper.selectById(646L)).thenReturn(new com.zhs.basket.domain.v4.BasketDivisionStage());
        when(scheduleBatchMapper.selectById(9L)).thenReturn(new com.zhs.basket.domain.v4.BasketScheduleBatch());

        support.advanceIfCompleted(qf);

        assertEquals(66L, sf.getId());
        assertEquals(296L, sf.getHomeEventTeamId());
        assertEquals(299L, sf.getAwayEventTeamId());
        assertEquals(LocalDate.of(2026, 9, 6), sf.getScheduledDate());
        assertEquals(LocalTime.of(21, 0), sf.getScheduledStartTime());
        verify(matchSeriesSupport).ensureGames(any(), any(), org.mockito.ArgumentMatchers.eq(sf),
                org.mockito.ArgumentMatchers.eq(296L), org.mockito.ArgumentMatchers.eq(299L));
    }

    private static BasketMatchSeries completed(Long id, Long winner) {
        BasketMatchSeries s = new BasketMatchSeries();
        s.setId(id);
        s.setWinnerEventTeamId(winner);
        s.setSeriesStatus(MatchSeriesStatusEnum.COMPLETED.getCode());
        return s;
    }

    private static BasketMatchSeries placeholder(Long id, Long homeSrc, Long awaySrc) {
        BasketMatchSeries s = new BasketMatchSeries();
        s.setId(id);
        s.setDivisionId(177L);
        s.setStageId(646L);
        s.setScheduleBatchId(9L);
        s.setSourceSeriesIdHome(homeSrc);
        s.setSourceSeriesIdAway(awaySrc);
        s.setHomeEventTeamId(0L);
        s.setAwayEventTeamId(0L);
        s.setScheduledDate(LocalDate.of(2026, 9, 6));
        s.setScheduledStartTime(LocalTime.of(21, 0));
        s.setScheduleStatus(ScheduleStatusEnum.SCHEDULED.getCode());
        return s;
    }

    private static Long positive(Long id) {
        return id == null || id <= 0 ? null : id;
    }
}
