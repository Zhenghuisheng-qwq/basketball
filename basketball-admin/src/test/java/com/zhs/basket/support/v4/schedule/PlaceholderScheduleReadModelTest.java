package com.zhs.basket.support.v4.schedule;

import com.zhs.basket.domain.v4.BasketMatchSeries;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.enums.v4.PublishStatusEnum;
import com.zhs.basket.enums.v4.ScheduleStatusEnum;
import com.zhs.basket.enums.v4.StructureStatusEnum;
import com.zhs.basket.mapper.v4.BasketEventTeamMapper;
import com.zhs.basket.mapper.v4.BasketMatchSeriesMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.vo.v4.ScheduleUnitView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceholderScheduleReadModelTest {

    @Mock
    private BasketMatchSeriesMapper matchSeriesMapper;
    @Mock
    private BasketV4MatchMapper matchMapper;
    @Mock
    private BasketEventTeamMapper eventTeamMapper;

    @InjectMocks
    private ScheduleUnitResolver resolver;

    @Test
    void placeholderSeries_mapsSavedScheduleAndAdjustFlag() {
        BasketScheduleBatch batch = new BasketScheduleBatch();
        batch.setId(55L);
        batch.setDivisionId(177L);
        batch.setStageId(646L);
        batch.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
        batch.setStructureStatus(StructureStatusEnum.ACTIVE.getCode());

        BasketMatchSeries series = new BasketMatchSeries();
        series.setId(66L);
        series.setHomeEventTeamId(null);
        series.setAwayEventTeamId(null);
        series.setHomeSourceType("WINNER_OF_SERIES");
        series.setAwaySourceType("WINNER_OF_SERIES");
        series.setSourceSeriesIdHome(62L);
        series.setSourceSeriesIdAway(63L);
        series.setScheduledDate(LocalDate.of(2026, 9, 6));
        series.setScheduledStartTime(LocalTime.of(21, 0));
        series.setScheduleStatus(ScheduleStatusEnum.SCHEDULED.getCode());
        series.setSeriesStatus("READY");
        series.setRoundNo(2);
        series.setBestOf(5);

        when(matchMapper.selectByStageId(646L)).thenReturn(List.of());
        when(matchSeriesMapper.selectByScheduleBatchId(55L)).thenReturn(List.of(series));
        when(eventTeamMapper.selectActiveByDivisionId(177L)).thenReturn(List.of());

        List<ScheduleUnitView> units = resolver.listUnits(batch);
        assertEquals(1, units.size());
        ScheduleUnitView view = units.get(0);
        assertEquals(66L, view.getUnitId());
        assertEquals("2026-09-06", view.getScheduledDate());
        assertEquals("21:00", view.getScheduledStartTime());
        assertEquals(ScheduleStatusEnum.SCHEDULED.getCode(), view.getScheduleStatus());
        assertEquals("TBD", view.getParticipantResolutionStatus());
        assertEquals("WINNER_OF_SERIES", view.getHomeSourceType());
        assertEquals("WINNER_OF_SERIES", view.getAwaySourceType());
        assertEquals("\u524d\u7f6e\u6bd4\u8d5b\u80dc\u8005", view.getHomeParticipantName());
        assertEquals("\u524d\u7f6e\u6bd4\u8d5b\u80dc\u8005", view.getAwayParticipantName());
        assertTrue(view.isRequired());
        assertFalse(view.isCanEditSchedule());
        assertTrue(view.isCanAdjustSchedule());
        assertTrue(PairingResolutionGuard.isSchedulable(view));
    }

    @Test
    void unpublishedPlaceholder_canEditScheduleWithoutResolvedPairing() {
        BasketScheduleBatch batch = new BasketScheduleBatch();
        batch.setId(55L);
        batch.setDivisionId(177L);
        batch.setStageId(646L);
        batch.setPublishStatus(PublishStatusEnum.DRAFT.getCode());
        batch.setStructureStatus(StructureStatusEnum.ACTIVE.getCode());

        BasketMatchSeries series = new BasketMatchSeries();
        series.setId(68L);
        series.setHomeEventTeamId(null);
        series.setAwayEventTeamId(null);
        series.setHomeSourceType("WINNER_OF_SERIES");
        series.setAwaySourceType("WINNER_OF_SERIES");
        series.setScheduleStatus(ScheduleStatusEnum.UNSCHEDULED.getCode());
        series.setSeriesStatus("READY");
        series.setRoundNo(3);
        series.setBestOf(7);

        when(matchMapper.selectByStageId(646L)).thenReturn(List.of());
        when(matchSeriesMapper.selectByScheduleBatchId(55L)).thenReturn(List.of(series));
        when(eventTeamMapper.selectActiveByDivisionId(177L)).thenReturn(List.of());

        ScheduleUnitView view = resolver.listUnits(batch).get(0);
        assertTrue(view.isCanEditSchedule());
        assertFalse(view.isCanAdjustSchedule());
        assertTrue(PairingResolutionGuard.isSchedulable(view));
    }
}
