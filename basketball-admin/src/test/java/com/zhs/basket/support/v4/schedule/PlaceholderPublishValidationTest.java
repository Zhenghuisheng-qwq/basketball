package com.zhs.basket.support.v4.schedule;

import com.zhs.basket.enums.v4.ScheduleStatusEnum;
import com.zhs.basket.enums.v4.ScheduleUnitTypeEnum;
import com.zhs.basket.support.v4.schedule.validator.ParticipantSourcePublishValidator;
import com.zhs.basket.support.v4.schedule.validator.ScheduleCompletenessPublishValidator;
import com.zhs.basket.vo.v4.ScheduleUnitView;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceholderPublishValidationTest {

    @Test
    void winnerOfSeriesPlaceholder_isWarningNotError() {
        ParticipantSourcePublishValidator validator = new ParticipantSourcePublishValidator();
        ScheduleUnitView unit = new ScheduleUnitView();
        unit.setUnitType(ScheduleUnitTypeEnum.MATCH_SERIES.getCode());
        unit.setUnitId(66L);
        unit.setRequired(true);
        unit.setHomeParticipantId(null);
        unit.setAwayParticipantId(null);
        unit.setHomeSourceType("WINNER_OF_SERIES");
        unit.setAwaySourceType("WINNER_OF_SERIES");
        unit.setHomeParticipantName("\u4e0a\u4e00\u8f6e\u80dc\u8005");
        unit.setAwayParticipantName("\u4e0a\u4e00\u8f6e\u80dc\u8005");
        unit.setParticipantResolutionStatus("TBD");
        unit.setScheduleStatus(ScheduleStatusEnum.SCHEDULED.getCode());
        StagePublishContext context = new StagePublishContext();
        context.setUnits(List.of(unit));
        ValidationResult result = new ValidationResult();
        validator.validate(context, result);
        assertFalse(result.hasErrors());
        assertEquals(1, result.warnings().size());
        assertEquals("PLACEHOLDER_TBD", result.warnings().get(0).getCode());
    }

    @Test
    void missingSourceAndTeam_isError() {
        ParticipantSourcePublishValidator validator = new ParticipantSourcePublishValidator();
        ScheduleUnitView unit = new ScheduleUnitView();
        unit.setUnitType(ScheduleUnitTypeEnum.MATCH_SERIES.getCode());
        unit.setUnitId(99L);
        unit.setRequired(true);
        unit.setHomeParticipantId(null);
        unit.setAwayParticipantId(null);
        unit.setParticipantResolutionStatus("UNRESOLVED");
        StagePublishContext context = new StagePublishContext();
        context.setUnits(List.of(unit));
        ValidationResult result = new ValidationResult();
        validator.validate(context, result);
        assertTrue(result.hasErrors());
        assertEquals("PARTICIPANT_SOURCE_INVALID", result.errors().get(0).getCode());
    }

    @Test
    void placeholderWithoutSchedule_isScheduleMissingError() {
        ScheduleCompletenessPublishValidator completeness = new ScheduleCompletenessPublishValidator();
        ParticipantSourcePublishValidator pairing = new ParticipantSourcePublishValidator();
        ScheduleUnitView unit = new ScheduleUnitView();
        unit.setUnitType(ScheduleUnitTypeEnum.MATCH_SERIES.getCode());
        unit.setUnitId(68L);
        unit.setRoundNo(3);
        unit.setRequired(true);
        unit.setHomeParticipantId(null);
        unit.setAwayParticipantId(null);
        unit.setHomeSourceType("WINNER_OF_SERIES");
        unit.setAwaySourceType("WINNER_OF_SERIES");
        unit.setParticipantResolutionStatus("TBD");
        unit.setScheduleStatus(ScheduleStatusEnum.UNSCHEDULED.getCode());
        StagePublishContext context = new StagePublishContext();
        context.setUnits(List.of(unit));
        ValidationResult result = new ValidationResult();
        completeness.validate(context, result);
        pairing.validate(context, result);
        assertTrue(result.hasErrors());
        assertEquals("SCHEDULE_MISSING", result.errors().get(0).getCode());
        assertEquals(1, result.warnings().size());
        assertEquals("PLACEHOLDER_TBD", result.warnings().get(0).getCode());
    }

    @Test
    void placeholderWithSchedule_allowsPublishWithWarningOnly() {
        ScheduleCompletenessPublishValidator completeness = new ScheduleCompletenessPublishValidator();
        ParticipantSourcePublishValidator pairing = new ParticipantSourcePublishValidator();
        ScheduleUnitView unit = new ScheduleUnitView();
        unit.setUnitType(ScheduleUnitTypeEnum.MATCH_SERIES.getCode());
        unit.setUnitId(68L);
        unit.setRoundNo(3);
        unit.setRequired(true);
        unit.setHomeParticipantId(null);
        unit.setAwayParticipantId(null);
        unit.setHomeSourceType("WINNER_OF_SERIES");
        unit.setAwaySourceType("WINNER_OF_SERIES");
        unit.setHomeParticipantName("SF1\u80dc\u8005");
        unit.setAwayParticipantName("SF2\u80dc\u8005");
        unit.setParticipantResolutionStatus("TBD");
        unit.setScheduleStatus(ScheduleStatusEnum.SCHEDULED.getCode());
        unit.setScheduledDate("2026-09-12");
        unit.setScheduledStartTime("21:00");
        StagePublishContext context = new StagePublishContext();
        context.setUnits(List.of(unit));
        ValidationResult result = new ValidationResult();
        completeness.validate(context, result);
        pairing.validate(context, result);
        assertFalse(result.hasErrors());
        assertEquals(1, result.warnings().size());
        assertEquals("PLACEHOLDER_TBD", result.warnings().get(0).getCode());
    }
}
