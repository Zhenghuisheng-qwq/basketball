package com.zhs.basket.support.v4.schedule;

import com.zhs.basket.enums.v4.ScheduleStatusEnum;
import com.zhs.basket.enums.v4.ScheduleUnitTypeEnum;
import com.zhs.basket.support.v4.schedule.validator.TeamTimeConflictValidator;
import com.zhs.basket.vo.v4.ScheduleUnitView;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceholderSeriesSchedulingTest {

    @Test
    void legalWinnerPlaceholder_canBeScheduledWithoutTeamIds() {
        assertTrue(PairingResolutionGuard.isLegalPlaceholder(
                null, null, "WINNER_OF_SERIES", "WINNER_OF_SERIES", 11L, 12L));
        ScheduleUnitView unit = tbdSeries(66L);
        unit.setCanEditSchedule(true);
        ScheduleMutableGuard.assertUnitEditable(unit);
    }

    @Test
    void batchIncludesTbdRequiredUnits() {
        ScheduleUnitView qf = resolvedSeries(62L);
        ScheduleUnitView sf = tbdSeries(66L);
        sf.setRoundNo(2);
        assertTrue(sf.isRequired());
        assertTrue(PairingResolutionGuard.isLegalPlaceholder(
                sf.getHomeParticipantId(), sf.getAwayParticipantId(),
                sf.getHomeSourceType(), sf.getAwaySourceType(), 62L, 63L));
        assertTrue(qf.isRequired());
    }

    @Test
    void sameSlotPlaceholder_doesNotErrorAsTeamConflict() {
        TeamTimeConflictValidator validator = new TeamTimeConflictValidator();
        ScheduleUnitView a = tbdSeries(66L);
        a.setScheduledDate("2026-09-06");
        a.setScheduledStartTime("21:00");
        a.setScheduleStatus(ScheduleStatusEnum.SCHEDULED.getCode());
        ScheduleUnitView b = tbdSeries(67L);
        b.setScheduledDate("2026-09-06");
        b.setScheduledStartTime("21:00");
        b.setScheduleStatus(ScheduleStatusEnum.SCHEDULED.getCode());
        ValidationResult result = new ValidationResult();
        validator.validate(new StagePublishContext(), List.of(a, b), null, null, result);
        assertFalse(result.hasErrors());
    }

    private static ScheduleUnitView tbdSeries(Long id) {
        ScheduleUnitView view = new ScheduleUnitView();
        view.setUnitType(ScheduleUnitTypeEnum.MATCH_SERIES.getCode());
        view.setUnitId(id);
        view.setRoundNo(2);
        view.setBestOf(5);
        view.setRequired(true);
        view.setHomeParticipantId(null);
        view.setAwayParticipantId(null);
        view.setHomeSourceType("WINNER_OF_SERIES");
        view.setAwaySourceType("WINNER_OF_SERIES");
        view.setParticipantResolutionStatus("TBD");
        view.setScheduleStatus(ScheduleStatusEnum.UNSCHEDULED.getCode());
        return view;
    }

    private static ScheduleUnitView resolvedSeries(Long id) {
        ScheduleUnitView view = tbdSeries(id);
        view.setRoundNo(1);
        view.setBestOf(3);
        view.setHomeParticipantId(296L);
        view.setAwayParticipantId(303L);
        view.setParticipantResolutionStatus("RESOLVED");
        return view;
    }
}
