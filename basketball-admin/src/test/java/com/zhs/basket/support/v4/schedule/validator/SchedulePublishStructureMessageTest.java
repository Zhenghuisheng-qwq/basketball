package com.zhs.basket.support.v4.schedule.validator;

import com.zhs.basket.domain.v4.BasketDivisionFormatSnapshot;
import com.zhs.basket.domain.v4.BasketDivisionRosterVersion;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.domain.v4.BasketStagePairingExecution;
import com.zhs.basket.enums.v4.RosterWorkflowModeEnum;
import com.zhs.basket.enums.v4.SnapshotStatusEnum;
import com.zhs.basket.enums.v4.StructureStatusEnum;
import com.zhs.basket.support.v4.schedule.StagePublishContext;
import com.zhs.basket.support.v4.schedule.ValidationIssue;
import com.zhs.basket.support.v4.schedule.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchedulePublishStructureMessageTest {

    private final RuleVersionPublishValidator ruleValidator = new RuleVersionPublishValidator();
    private final RosterVersionPublishValidator rosterValidator = new RosterVersionPublishValidator();
    private final StructureActivePublishValidator structureValidator = new StructureActivePublishValidator();
    private final PairingExecutionPublishValidator pairingValidator = new PairingExecutionPublishValidator();

    @Test
    void missingStructureDoesNotReportVersionMismatch() {
        ValidationResult result = validate(context(null, false, "ROUND_ROBIN", null, null));

        assertFalse(result.canPublish());
        assertTrue(codes(result).contains("STRUCTURE_MISSING"));
        assertTrue(messages(result).contains(StructureActivePublishValidator.MSG_STRUCTURE_NOT_GENERATED));
        assertFalse(codes(result).contains("RULE_BINDING_MISMATCH"));
        assertFalse(codes(result).contains("ROSTER_BINDING_MISMATCH"));
        assertFalse(codes(result).contains("PAIRING_NOT_CONFIRMED"));
    }

    @Test
    void unlockedRosterStillBlocksWhenStructureIsMissing() {
        StagePublishContext context = context(null, false, "ROUND_ROBIN", null, null);
        context.getRosterVersion().setStatus("DRAFT");

        ValidationResult result = validate(context);

        assertFalse(result.canPublish());
        assertTrue(codes(result).contains("ROSTER_NOT_LOCKED"));
        assertFalse(codes(result).contains("ROSTER_BINDING_MISMATCH"));
        assertFalse(codes(result).contains("RULE_BINDING_MISMATCH"));
    }

    @Test
    void staleHistoryAsksToRegenerateAndDoesNotCompareVersions() {
        ValidationResult result = validate(context(null, true, "SINGLE_ELIMINATION", "RANDOM_CONFIRM", null));

        assertFalse(result.canPublish());
        assertTrue(messages(result).contains(StructureActivePublishValidator.MSG_STALE_REQUIRES_REGENERATE));
        assertFalse(messages(result).contains(StructureActivePublishValidator.MSG_STRUCTURE_NOT_GENERATED));
        assertFalse(codes(result).contains("RULE_BINDING_MISMATCH"));
        assertFalse(codes(result).contains("ROSTER_BINDING_MISMATCH"));
    }

    @Test
    void activeBatchWithWrongVersionsStaysBlocked() {
        BasketScheduleBatch batch = activeBatch(99L, 88L);
        ValidationResult result = validate(context(batch, false, "SINGLE_ELIMINATION", "RANDOM_CONFIRM", confirmedPairing()));

        assertFalse(result.canPublish());
        assertTrue(codes(result).contains("RULE_BINDING_MISMATCH"));
        assertTrue(codes(result).contains("ROSTER_BINDING_MISMATCH"));
        assertFalse(codes(result).contains("STRUCTURE_MISSING"));
    }

    @Test
    void activeBatchBoundToCurrentVersionsCanPassVersionChecks() {
        BasketScheduleBatch batch = activeBatch(10L, 20L);
        ValidationResult result = validate(context(batch, false, "ROUND_ROBIN", null, null));

        assertTrue(result.canPublish());
        assertFalse(codes(result).contains("RULE_BINDING_MISMATCH"));
        assertFalse(codes(result).contains("ROSTER_BINDING_MISMATCH"));
        assertFalse(codes(result).contains("STRUCTURE_MISSING"));
    }

    @Test
    void arenaWithoutConfirmedDrawStillRequiresDraw() {
        ValidationResult result = validate(context(null, false, "SINGLE_ELIMINATION", "RANDOM_CONFIRM", draftPairing()));

        assertFalse(result.canPublish());
        assertTrue(codes(result).contains("PAIRING_NOT_CONFIRMED"));
        assertTrue(codes(result).contains("STRUCTURE_MISSING"));
        assertFalse(codes(result).contains("RULE_BINDING_MISMATCH"));
    }

    @Test
    void roundRobinDoesNotRequireDraw() {
        ValidationResult result = validate(context(activeBatch(10L, 20L), false, "ROUND_ROBIN", null, null));

        assertTrue(result.canPublish());
        assertFalse(codes(result).contains("PAIRING_NOT_CONFIRMED"));
    }

    private ValidationResult validate(StagePublishContext context) {
        ValidationResult result = new ValidationResult();
        ruleValidator.validate(context, result);
        rosterValidator.validate(context, result);
        structureValidator.validate(context, result);
        pairingValidator.validate(context, result);
        return result;
    }

    private StagePublishContext context(BasketScheduleBatch batch, boolean historicalStale,
                                        String format, String drawRule, BasketStagePairingExecution pairing) {
        BasketDivisionFormatSnapshot rule = new BasketDivisionFormatSnapshot();
        rule.setId(10L);
        rule.setSnapshotStatus(SnapshotStatusEnum.LOCKED.getCode());
        rule.setRosterWorkflowMode(RosterWorkflowModeEnum.VERSIONED.getCode());

        BasketDivisionRosterVersion roster = new BasketDivisionRosterVersion();
        roster.setId(20L);
        roster.setStatus("LOCKED");

        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setId(30L);
        stage.setSnapshotId(10L);
        stage.setCompetitionFormat(format);
        stage.setDrawRule(drawRule);

        StagePublishContext context = new StagePublishContext();
        context.setRuleVersion(rule);
        context.setRosterVersion(roster);
        context.setStage(stage);
        context.setStructureBatch(batch);
        context.setHistoricalStaleStructure(historicalStale);
        context.setPairingExecution(pairing);
        return context;
    }

    private BasketScheduleBatch activeBatch(Long ruleId, Long rosterId) {
        BasketScheduleBatch batch = new BasketScheduleBatch();
        batch.setId(40L);
        batch.setStageId(30L);
        batch.setRuleVersionId(ruleId);
        batch.setRosterVersionId(rosterId);
        batch.setStructureStatus(StructureStatusEnum.ACTIVE.getCode());
        return batch;
    }

    private BasketStagePairingExecution confirmedPairing() {
        BasketStagePairingExecution pairing = new BasketStagePairingExecution();
        pairing.setStatus("CONFIRMED");
        return pairing;
    }

    private BasketStagePairingExecution draftPairing() {
        BasketStagePairingExecution pairing = new BasketStagePairingExecution();
        pairing.setStatus("DRAFT");
        return pairing;
    }

    private Set<String> codes(ValidationResult result) {
        return result.errors().stream().map(ValidationIssue::getCode).collect(Collectors.toSet());
    }

    private Set<String> messages(ValidationResult result) {
        return result.errors().stream().map(ValidationIssue::getMessage).collect(Collectors.toSet());
    }
}
