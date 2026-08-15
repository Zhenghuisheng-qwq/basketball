package com.zhs.basket.support.v4.schedule;

import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.enums.v4.MatchSeriesStatusEnum;
import com.zhs.basket.enums.v4.MatchStatusEnum;
import com.zhs.basket.enums.v4.PublishStatusEnum;
import com.zhs.basket.enums.v4.ScheduleUnitTypeEnum;
import com.zhs.basket.enums.v4.StagePublishActionTypeEnum;
import com.zhs.basket.vo.v4.ScheduleUnitView;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleAdjustmentGuardTest {

    @Test
    void reasonRequired() {
        ServiceException ex = assertThrows(ServiceException.class, () -> ScheduleAdjustmentGuard.assertReason("  "));
        assertEquals(ScheduleAdjustmentGuard.MSG_REASON_REQUIRED, ex.getMessage());
    }

    @Test
    void unpublishedRejected() {
        BasketScheduleBatch batch = new BasketScheduleBatch();
        batch.setPublishStatus(PublishStatusEnum.DRAFT.getCode());
        ServiceException ex = assertThrows(ServiceException.class, () -> ScheduleAdjustmentGuard.assertPublished(batch));
        assertEquals(ScheduleAdjustmentGuard.MSG_NOT_PUBLISHED, ex.getMessage());
    }

    @Test
    void seriesReadyCanAdjust_evenWithoutTeams() {
        ScheduleUnitView unit = series(MatchSeriesStatusEnum.READY.getCode());
        unit.setHomeParticipantId(null);
        unit.setAwayParticipantId(null);
        assertTrue(ScheduleAdjustmentGuard.canAdjust(unit, false));
        assertDoesNotThrow(() -> ScheduleAdjustmentGuard.assertCanAdjust(unit, false));
    }

    @Test
    void seriesRunningRejected() {
        ScheduleUnitView unit = series(MatchSeriesStatusEnum.RUNNING.getCode());
        ServiceException ex = assertThrows(ServiceException.class, () -> ScheduleAdjustmentGuard.assertCanAdjust(unit, false));
        assertEquals(ScheduleAdjustmentGuard.MSG_SERIES_STARTED, ex.getMessage());
    }

    @Test
    void seriesChildFinishedRejected() {
        ScheduleUnitView unit = series(MatchSeriesStatusEnum.READY.getCode());
        ServiceException ex = assertThrows(ServiceException.class, () -> ScheduleAdjustmentGuard.assertCanAdjust(unit, true));
        assertEquals(ScheduleAdjustmentGuard.MSG_SERIES_STARTED, ex.getMessage());
    }

    @Test
    void seriesCompletedRejected() {
        ScheduleUnitView unit = series(MatchSeriesStatusEnum.COMPLETED.getCode());
        ServiceException ex = assertThrows(ServiceException.class, () -> ScheduleAdjustmentGuard.assertCanAdjust(unit, false));
        assertEquals(ScheduleAdjustmentGuard.MSG_SERIES_FINISHED, ex.getMessage());
    }

    @Test
    void matchPlayingRejected() {
        ScheduleUnitView unit = match(MatchStatusEnum.PLAYING.getCode());
        ServiceException ex = assertThrows(ServiceException.class, () -> ScheduleAdjustmentGuard.assertCanAdjust(unit, false));
        assertEquals(ScheduleAdjustmentGuard.MSG_MATCH_STARTED, ex.getMessage());
    }

    @Test
    void matchFinishedRejected() {
        ScheduleUnitView unit = match(MatchStatusEnum.FINISHED.getCode());
        ServiceException ex = assertThrows(ServiceException.class, () -> ScheduleAdjustmentGuard.assertCanAdjust(unit, false));
        assertEquals(ScheduleAdjustmentGuard.MSG_MATCH_FINISHED, ex.getMessage());
    }

    @Test
    void auditKeepsOldAndNewTime() {
        String reason = ScheduleAdjustmentGuard.formatAuditReason(
                "MATCH_SERIES", 32L, "2026-08-16", "21:00:00", "2026-08-17", "22:00", "E2E Reschedule Test");
        assertTrue(reason.contains("2026-08-16"));
        assertTrue(reason.contains("21:00"));
        assertTrue(reason.contains("2026-08-17"));
        assertTrue(reason.contains("22:00"));
        assertTrue(reason.contains("E2E Reschedule Test"));
        assertTrue(reason.startsWith("ADJUST MATCH_SERIES:32"));
    }

    @Test
    void scheduleAdjustKeepsPublished() {
        assertEquals(PublishStatusEnum.PUBLISHED,
                StagePublishStatusTransition.next(PublishStatusEnum.PUBLISHED, StagePublishActionTypeEnum.SCHEDULE_ADJUST));
    }

    private ScheduleUnitView series(String status) {
        ScheduleUnitView unit = new ScheduleUnitView();
        unit.setUnitType(ScheduleUnitTypeEnum.MATCH_SERIES.getCode());
        unit.setUnitId(32L);
        unit.setRequired(true);
        unit.setSeriesStatus(status);
        return unit;
    }

    private ScheduleUnitView match(String status) {
        ScheduleUnitView unit = new ScheduleUnitView();
        unit.setUnitType(ScheduleUnitTypeEnum.MATCH.getCode());
        unit.setUnitId(1L);
        unit.setRequired(true);
        unit.setMatchStatus(status);
        return unit;
    }
}
