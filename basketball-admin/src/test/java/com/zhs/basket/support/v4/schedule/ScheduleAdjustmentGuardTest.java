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
    void withdrawnRescheduleRejected_draftEditAllowed() {
        BasketScheduleBatch withdrawn = new BasketScheduleBatch();
        withdrawn.setPublishStatus(PublishStatusEnum.WITHDRAWN.getCode());
        withdrawn.setStructureStatus(com.zhs.basket.enums.v4.StructureStatusEnum.ACTIVE.getCode());
        ServiceException reschedule = assertThrows(ServiceException.class,
                () -> ScheduleAdjustmentGuard.assertPublished(withdrawn));
        assertEquals(ScheduleAdjustmentGuard.MSG_NOT_PUBLISHED, reschedule.getMessage());
        assertDoesNotThrow(() -> ScheduleMutableGuard.assertNotPublished(withdrawn));
        assertDoesNotThrow(() -> ScheduleMutableGuard.assertStructureActive(withdrawn));
    }

    @Test
    void publishedAssignRejected_rescheduleAllowedForNotStarted() {
        BasketScheduleBatch published = new BasketScheduleBatch();
        published.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
        published.setStructureStatus(com.zhs.basket.enums.v4.StructureStatusEnum.ACTIVE.getCode());
        ServiceException assign = assertThrows(ServiceException.class,
                () -> ScheduleMutableGuard.assertNotPublished(published));
        assertTrue(assign.getMessage().contains("比赛执行 → 比赛管理"));
        assertDoesNotThrow(() -> ScheduleAdjustmentGuard.assertPublished(published));
        ScheduleUnitView unit = series(MatchSeriesStatusEnum.READY.getCode());
        assertDoesNotThrow(() -> ScheduleAdjustmentGuard.assertCanAdjust(unit, false));
    }

    @Test
    void publishedAssignmentValidator_doesNotRequireWithdraw() {
        com.zhs.basket.support.v4.schedule.validator.StructureActiveScheduleValidator validator =
                new com.zhs.basket.support.v4.schedule.validator.StructureActiveScheduleValidator();
        BasketScheduleBatch published = new BasketScheduleBatch();
        published.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
        published.setStructureStatus(com.zhs.basket.enums.v4.StructureStatusEnum.ACTIVE.getCode());
        StagePublishContext context = new StagePublishContext();
        context.setStructureBatch(published);
        ValidationResult result = new ValidationResult();
        validator.validate(context, java.util.List.of(), null, null, result);
        assertFalse(result.hasErrors());
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
    void matchManagementAllowsFutureSeriesWhenOthersCompleted() {
        assertTrue(ScheduleAdjustmentGuard.canAdjustInMatchManagement(
                PublishStatusEnum.PUBLISHED.getCode(), MatchSeriesStatusEnum.READY.getCode(), false));
        assertTrue(ScheduleAdjustmentGuard.canAdjustInMatchManagement(
                PublishStatusEnum.PUBLISHED.getCode(), MatchSeriesStatusEnum.PENDING.getCode(), false));
        assertNull(ScheduleAdjustmentGuard.matchManagementBlockReason(
                PublishStatusEnum.PUBLISHED.getCode(), MatchSeriesStatusEnum.READY.getCode(), false));
    }

    @Test
    void matchManagementRejectsUnpublishedAndStarted() {
        assertEquals(ScheduleAdjustmentGuard.MSG_NOT_OPERATIONAL,
                ScheduleAdjustmentGuard.matchManagementBlockReason(
                        PublishStatusEnum.WITHDRAWN.getCode(), MatchSeriesStatusEnum.READY.getCode(), false));
        assertEquals(ScheduleAdjustmentGuard.MSG_SERIES_STARTED,
                ScheduleAdjustmentGuard.matchManagementBlockReason(
                        PublishStatusEnum.PUBLISHED.getCode(), MatchSeriesStatusEnum.RUNNING.getCode(), false));
        assertEquals(ScheduleAdjustmentGuard.MSG_SERIES_STARTED,
                ScheduleAdjustmentGuard.matchManagementBlockReason(
                        PublishStatusEnum.PUBLISHED.getCode(), MatchSeriesStatusEnum.READY.getCode(), true));
        assertEquals(ScheduleAdjustmentGuard.MSG_SERIES_FINISHED,
                ScheduleAdjustmentGuard.matchManagementBlockReason(
                        PublishStatusEnum.PUBLISHED.getCode(), MatchSeriesStatusEnum.COMPLETED.getCode(), false));
    }

    @Test
    void standaloneBo1PublishedScheduled_canAdjust() {
        assertTrue(ScheduleAdjustmentGuard.canAdjustStandaloneMatch(
                null, PublishStatusEnum.PUBLISHED.getCode(), MatchStatusEnum.SCHEDULED.getCode()));
        assertTrue(ScheduleAdjustmentGuard.canAdjustStandaloneMatch(
                0L, PublishStatusEnum.PUBLISHED.getCode(), MatchStatusEnum.SCHEDULED.getCode()));
        assertFalse(ScheduleAdjustmentGuard.isSeriesChildMatch(null));
        assertFalse(ScheduleAdjustmentGuard.isSeriesChildMatch(0L));
        assertTrue(ScheduleAdjustmentGuard.isSeriesChildMatch(32L));
    }

    @Test
    void standaloneBo1InProgress_rejected() {
        assertEquals(ScheduleAdjustmentGuard.MSG_MATCH_STARTED,
                ScheduleAdjustmentGuard.matchManagementMatchBlockReason(
                        null, PublishStatusEnum.PUBLISHED.getCode(), MatchStatusEnum.PLAYING.getCode()));
    }

    @Test
    void childMatchIndependentAdjust_rejected() {
        assertEquals(ScheduleAdjustmentGuard.MSG_CHILD_MATCH,
                ScheduleAdjustmentGuard.matchManagementMatchBlockReason(
                        32L, PublishStatusEnum.PUBLISHED.getCode(), MatchStatusEnum.SCHEDULED.getCode()));
        ServiceException ex = assertThrows(ServiceException.class, () -> ScheduleAdjustmentGuard.assertStandaloneMatch(32L));
        assertEquals(ScheduleAdjustmentGuard.MSG_CHILD_MATCH, ex.getMessage());
    }

    @Test
    void standaloneBo1Finished_rejected() {
        assertEquals(ScheduleAdjustmentGuard.MSG_MATCH_FINISHED,
                ScheduleAdjustmentGuard.matchManagementMatchBlockReason(
                        null, PublishStatusEnum.PUBLISHED.getCode(), MatchStatusEnum.FINISHED.getCode()));
    }

    @Test
    void futureStandaloneBo1AllowedWhenOtherMatchesRunning() {
        assertTrue(ScheduleAdjustmentGuard.canAdjustStandaloneMatch(
                null, PublishStatusEnum.PUBLISHED.getCode(), MatchStatusEnum.SCHEDULED.getCode()));
        assertEquals(ScheduleAdjustmentGuard.MSG_MATCH_STARTED,
                ScheduleAdjustmentGuard.matchManagementMatchBlockReason(
                        null, PublishStatusEnum.PUBLISHED.getCode(), MatchStatusEnum.PLAYING.getCode()));
    }

    @Test
    void unpublishedMatchGoesBackToWorkbench() {
        assertEquals(ScheduleAdjustmentGuard.MSG_NOT_OPERATIONAL,
                ScheduleAdjustmentGuard.matchManagementMatchBlockReason(
                        null, PublishStatusEnum.DRAFT.getCode(), MatchStatusEnum.SCHEDULED.getCode()));
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
