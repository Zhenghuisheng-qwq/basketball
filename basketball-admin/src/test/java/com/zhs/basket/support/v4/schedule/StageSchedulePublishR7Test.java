package com.zhs.basket.support.v4.schedule;

import com.zhs.basket.domain.v4.BasketDivisionFormatSnapshot;
import com.zhs.basket.domain.v4.BasketDivisionRosterVersion;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketMatchSeries;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.enums.v4.PublishStatusEnum;
import com.zhs.basket.enums.v4.ScheduleRecommendationTypeEnum;
import com.zhs.basket.enums.v4.ScheduleSourceEnum;
import com.zhs.basket.enums.v4.ScheduleStatusEnum;
import com.zhs.basket.enums.v4.ScheduleUnitTypeEnum;
import com.zhs.basket.enums.v4.StagePublishActionTypeEnum;
import com.zhs.basket.enums.v4.StructureStatusEnum;
import com.zhs.basket.support.v4.schedule.validator.ParticipantSourcePublishValidator;
import com.zhs.basket.support.v4.schedule.validator.RosterVersionPublishValidator;
import com.zhs.basket.support.v4.schedule.validator.RuleVersionPublishValidator;
import com.zhs.basket.support.v4.schedule.validator.ScheduleCompletenessPublishValidator;
import com.zhs.basket.support.v4.schedule.validator.StructureActivePublishValidator;
import com.zhs.basket.support.v4.schedule.validator.TeamTimeConflictValidator;
import com.zhs.basket.vo.v4.ScheduleUnitView;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StageSchedulePublishR7Test {

    @Test
    void fingerprint_isStableAndIgnoresNaturalOrder() {
        ScheduleUnitView a = unit("MATCH", 2L, "2026-10-01", "21:00");
        ScheduleUnitView b = unit("MATCH", 1L, "2026-10-02", "22:00");
        String left = ScheduleFingerprintSupport.fingerprint(List.of(a, b));
        String right = ScheduleFingerprintSupport.fingerprint(List.of(b, a));
        assertEquals(left, right);
        assertEquals(64, left.length());
    }

    @Test
    void recommendation_isDeterministicAndDoesNotOverwriteManual() {
        RoundRobinScheduleRecommendationStrategy strategy = new RoundRobinScheduleRecommendationStrategy();
        List<ScheduleUnitView> units = new ArrayList<>();
        units.add(required(11L, 1, null, null));
        units.add(required(12L, 1, null, null));
        units.add(required(21L, 2, "2026-10-08", "21:00"));
        units.get(2).setScheduleStatus(ScheduleStatusEnum.SCHEDULED.getCode());
        units.get(2).setScheduleSource(ScheduleSourceEnum.MANUAL.getCode());

        LocalDate start = LocalDate.of(2026, 10, 5); // Monday -> first Tue 10-06
        List<ScheduleUnitView> first = strategy.recommend(units, start, false);
        List<ScheduleUnitView> second = strategy.recommend(units, start, false);
        assertEquals(first.get(0).getScheduledDate(), second.get(0).getScheduledDate());
        assertEquals("2026-10-06", first.get(0).getScheduledDate());
        assertEquals("21:00", first.get(0).getScheduledStartTime());
        assertEquals("2026-10-08", first.get(2).getScheduledDate());
        assertEquals(ScheduleSourceEnum.MANUAL.getCode(), first.get(2).getScheduleSource());
    }

    @Test
    void recommendation_round2UsesSecondSlotSameDay() {
        RoundRobinScheduleRecommendationStrategy strategy = new RoundRobinScheduleRecommendationStrategy();
        List<ScheduleUnitView> units = List.of(required(1L, 1, null, null), required(2L, 2, null, null));
        List<ScheduleUnitView> rec = strategy.recommend(units, LocalDate.of(2026, 10, 6), true);
        assertEquals("2026-10-06", rec.get(0).getScheduledDate());
        assertEquals("21:00", rec.get(0).getScheduledStartTime());
        assertEquals("2026-10-06", rec.get(1).getScheduledDate());
        assertEquals("22:00", rec.get(1).getScheduledStartTime());
    }

    @Test
    void teamConflict_sameTeamSameSlotRejected_parallelDifferentTeamsAllowed() {
        TeamTimeConflictValidator validator = new TeamTimeConflictValidator();
        ScheduleUnitView m1 = required(1L, 1, "2026-10-01", "21:00");
        m1.setHomeParticipantId(10L);
        m1.setAwayParticipantId(20L);
        m1.setScheduleStatus(ScheduleStatusEnum.SCHEDULED.getCode());
        ScheduleUnitView m2 = required(2L, 1, "2026-10-01", "21:00");
        m2.setHomeParticipantId(10L);
        m2.setAwayParticipantId(30L);
        m2.setScheduleStatus(ScheduleStatusEnum.SCHEDULED.getCode());
        ValidationResult conflict = new ValidationResult();
        validator.validate(new StagePublishContext(), List.of(m1, m2), null, null, conflict);
        assertTrue(conflict.hasErrors());

        ScheduleUnitView m3 = required(3L, 1, "2026-10-01", "21:00");
        m3.setHomeParticipantId(40L);
        m3.setAwayParticipantId(50L);
        m3.setScheduleStatus(ScheduleStatusEnum.SCHEDULED.getCode());
        ValidationResult parallel = new ValidationResult();
        validator.validate(new StagePublishContext(), List.of(m1, m3), null, null, parallel);
        assertFalse(parallel.hasErrors());
    }

    @Test
    void unresolvedPlaceholder_doesNotFabricateTeamCollision() {
        TeamTimeConflictValidator validator = new TeamTimeConflictValidator();
        ScheduleUnitView m1 = required(1L, 1, "2026-10-01", "21:00");
        m1.setHomeParticipantId(null);
        m1.setAwayParticipantId(null);
        m1.setScheduleStatus(ScheduleStatusEnum.SCHEDULED.getCode());
        ScheduleUnitView m2 = required(2L, 1, "2026-10-01", "21:00");
        m2.setHomeParticipantId(null);
        m2.setAwayParticipantId(null);
        m2.setScheduleStatus(ScheduleStatusEnum.SCHEDULED.getCode());
        ValidationResult result = new ValidationResult();
        validator.validate(new StagePublishContext(), List.of(m1, m2), null, null, result);
        assertFalse(result.hasErrors());
    }

    @Test
    void bo3AndBo7_seriesScheduleIsEnoughForCompleteness() {
        ScheduleCompletenessPublishValidator validator = new ScheduleCompletenessPublishValidator();
        ScheduleUnitView bo3 = series(101L, 3, "2026-10-01", "21:00");
        ScheduleUnitView bo7 = series(102L, 7, "2026-11-10", "21:00");
        StagePublishContext context = new StagePublishContext();
        context.setUnits(List.of(bo3, bo7));
        ValidationResult result = new ValidationResult();
        validator.validate(context, result);
        assertFalse(result.hasErrors());
    }

    @Test
    void completeness_blocksUnscheduledRequiredUnit() {
        ScheduleCompletenessPublishValidator validator = new ScheduleCompletenessPublishValidator();
        ScheduleUnitView missing = series(101L, 3, null, null);
        missing.setScheduleStatus(ScheduleStatusEnum.UNSCHEDULED.getCode());
        StagePublishContext context = new StagePublishContext();
        context.setUnits(List.of(missing));
        ValidationResult result = new ValidationResult();
        validator.validate(context, result);
        assertTrue(result.hasErrors());
        assertEquals("SCHEDULE_MISSING", result.errors().get(0).getCode());
    }

    @Test
    void staleStructure_cannotPublish() {
        StructureActivePublishValidator validator = new StructureActivePublishValidator();
        BasketScheduleBatch batch = new BasketScheduleBatch();
        batch.setId(1L);
        batch.setStructureStatus(StructureStatusEnum.STALE.getCode());
        StagePublishContext context = new StagePublishContext();
        context.setStructureBatch(batch);
        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setId(9L);
        stage.setScheduleBatchId(1L);
        context.setStage(stage);
        ValidationResult result = new ValidationResult();
        validator.validate(context, result);
        assertTrue(result.hasErrors());
        assertEquals("STRUCTURE_STALE", result.errors().get(0).getCode());
    }

    @Test
    void publishedGuard_rejectsScheduleEdit() {
        BasketScheduleBatch batch = new BasketScheduleBatch();
        batch.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
        assertThrows(ServiceException.class, () -> ScheduleMutableGuard.assertNotPublished(batch));
    }

    @Test
    void publishTransition_draftToPublishedToWithdrawnToPublished() {
        assertEquals(PublishStatusEnum.PUBLISHED,
                StagePublishStatusTransition.next(PublishStatusEnum.DRAFT, StagePublishActionTypeEnum.PUBLISH));
        assertEquals(PublishStatusEnum.WITHDRAWN,
                StagePublishStatusTransition.next(PublishStatusEnum.PUBLISHED, StagePublishActionTypeEnum.WITHDRAW));
        assertEquals(PublishStatusEnum.PUBLISHED,
                StagePublishStatusTransition.next(PublishStatusEnum.WITHDRAWN, StagePublishActionTypeEnum.REPUBLISH));
        assertThrows(ServiceException.class,
                () -> StagePublishStatusTransition.next(PublishStatusEnum.DRAFT, StagePublishActionTypeEnum.WITHDRAW));
    }

    @Test
    void deterministicPlaceholder_isWarningNotError() {
        ParticipantSourcePublishValidator validator = new ParticipantSourcePublishValidator();
        ScheduleUnitView unit = series(9L, 1, "2026-11-01", "21:00");
        unit.setHomeParticipantId(null);
        unit.setAwayParticipantId(null);
        unit.setHomeSourceType("GROUP_RANK");
        unit.setAwaySourceType("GROUP_RANK");
        unit.setHomeParticipantName("A组第1名");
        unit.setAwayParticipantName("D组第2名");
        unit.setParticipantResolutionStatus("TBD");
        StagePublishContext context = new StagePublishContext();
        context.setUnits(List.of(unit));
        ValidationResult result = new ValidationResult();
        validator.validate(context, result);
        assertFalse(result.hasErrors());
        assertEquals(1, result.warnings().size());
        assertEquals("PLACEHOLDER_TBD", result.warnings().get(0).getCode());
    }

    @Test
    void missingSource_isError() {
        ParticipantSourcePublishValidator validator = new ParticipantSourcePublishValidator();
        ScheduleUnitView unit = series(9L, 1, "2026-11-01", "21:00");
        unit.setHomeParticipantId(null);
        unit.setAwayParticipantId(null);
        unit.setHomeSourceType(null);
        unit.setAwaySourceType(null);
        unit.setParticipantResolutionStatus("UNRESOLVED");
        StagePublishContext context = new StagePublishContext();
        context.setUnits(List.of(unit));
        ValidationResult result = new ValidationResult();
        validator.validate(context, result);
        assertTrue(result.hasErrors());
        assertEquals("PARTICIPANT_SOURCE_INVALID", result.errors().get(0).getCode());
    }

    @Test
    void versionedRuleNotLocked_blocksPublish() {
        RuleVersionPublishValidator validator = new RuleVersionPublishValidator();
        BasketDivisionFormatSnapshot rule = new BasketDivisionFormatSnapshot();
        rule.setId(11L);
        rule.setRosterWorkflowMode("VERSIONED");
        rule.setSnapshotStatus("DRAFT");
        BasketScheduleBatch batch = new BasketScheduleBatch();
        batch.setRuleVersionId(11L);
        StagePublishContext context = new StagePublishContext();
        context.setRuleVersion(rule);
        context.setStructureBatch(batch);
        ValidationResult result = new ValidationResult();
        validator.validate(context, result);
        assertTrue(result.hasErrors());
        assertEquals("RULE_NOT_LOCKED", result.errors().get(0).getCode());
    }

    @Test
    void rosterBindingMismatch_blocksPublish() {
        RosterVersionPublishValidator validator = new RosterVersionPublishValidator();
        BasketDivisionFormatSnapshot rule = new BasketDivisionFormatSnapshot();
        rule.setRosterWorkflowMode("VERSIONED");
        BasketDivisionRosterVersion roster = new BasketDivisionRosterVersion();
        roster.setId(202L);
        roster.setStatus("LOCKED");
        BasketScheduleBatch batch = new BasketScheduleBatch();
        batch.setRosterVersionId(201L);
        StagePublishContext context = new StagePublishContext();
        context.setRuleVersion(rule);
        context.setRosterVersion(roster);
        context.setStructureBatch(batch);
        ValidationResult result = new ValidationResult();
        validator.validate(context, result);
        assertTrue(result.hasErrors());
        assertEquals("ROSTER_BINDING_MISMATCH", result.errors().get(0).getCode());
    }

    @Test
    void factory_roundRobinUngrouped_notSeriesIfElse() {
        ScheduleRecommendationStrategyFactory factory = new ScheduleRecommendationStrategyFactory();
        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setCompetitionFormat("ROUND_ROBIN");
        stage.setOrganizationMode("UNGROUPED");
        assertEquals(ScheduleRecommendationTypeEnum.ROUND_ROBIN_STANDARD, factory.resolveType(stage));
        stage.setCompetitionFormat("SINGLE_ELIMINATION");
        assertEquals(ScheduleRecommendationTypeEnum.MANUAL_ONLY, factory.resolveType(stage));
    }

    @Test
    void winnerAdvance_doesNotClearScheduleFields() {
        BasketMatchSeries next = new BasketMatchSeries();
        next.setScheduledDate(java.time.LocalDate.of(2026, 11, 3));
        next.setScheduledStartTime(java.time.LocalTime.of(21, 0));
        next.setScheduleStatus(ScheduleStatusEnum.SCHEDULED.getCode());
        next.setHomeEventTeamId(0L);
        assertEquals(java.time.LocalDate.of(2026, 11, 3), next.getScheduledDate());
        assertEquals(java.time.LocalTime.of(21, 0), next.getScheduledStartTime());
    }

    private ScheduleUnitView unit(String type, Long id, String date, String time) {
        ScheduleUnitView view = new ScheduleUnitView();
        view.setUnitType(type);
        view.setUnitId(id);
        view.setScheduledDate(date);
        view.setScheduledStartTime(time);
        return view;
    }

    private ScheduleUnitView required(Long id, int round, String date, String time) {
        ScheduleUnitView view = unit(ScheduleUnitTypeEnum.MATCH.getCode(), id, date, time);
        view.setRoundNo(round);
        view.setMatchOrder(id.intValue());
        view.setRequired(true);
        return view;
    }

    private ScheduleUnitView series(Long id, int bestOf, String date, String time) {
        ScheduleUnitView view = unit(ScheduleUnitTypeEnum.MATCH_SERIES.getCode(), id, date, time);
        view.setRoundNo(1);
        view.setBestOf(bestOf);
        view.setRequired(true);
        view.setScheduleStatus(date == null ? ScheduleStatusEnum.UNSCHEDULED.getCode() : ScheduleStatusEnum.SCHEDULED.getCode());
        return view;
    }
}
