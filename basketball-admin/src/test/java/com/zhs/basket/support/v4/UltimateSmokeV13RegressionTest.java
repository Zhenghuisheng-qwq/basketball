package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketDivisionFormatSnapshot;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.basket.enums.v4.PairingRuleEnum;
import com.zhs.basket.enums.v4.RegistrationModeEnum;
import com.zhs.basket.enums.v4.SnapshotStatusEnum;
import com.zhs.basket.support.v4.bracket.EliminationCapacity;
import com.zhs.basket.support.v4.bracket.GroupCrossPlacementRequest;
import com.zhs.basket.support.v4.bracket.GroupCrossPlacementStrategy;
import com.zhs.basket.support.v4.bracket.GroupLayout;
import com.zhs.basket.support.v4.stage.StageSchedulePrerequisiteValidator;
import com.zhs.basket.support.v4.stage.StructureGenerationPrerequisiteValidator;
import com.zhs.basket.support.v4.template.ArenaStandardTemplateBuilder;
import com.zhs.basket.support.v4.template.CupStandardTemplateBuilder;
import com.zhs.basket.support.v4.template.LeagueStandardTemplateBuilder;
import com.zhs.basket.support.v4.template.OpenStandardTemplateBuilder;
import com.zhs.basket.support.v4.template.StandardFormatTemplateFactory;
import com.zhs.basket.support.v4.template.StandardTemplateCodes;
import com.zhs.basket.support.v4.template.StandardTemplateContext;
import com.zhs.basket.support.v4.template.StandardTemplateDefinition;
import com.zhs.basket.support.v4.version.RuleVersionSupport;
import com.zhs.basket.support.v4.version.RuleVersionView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * V1.3 Ultimate Smoke gate: cross-series Rule graphs, series counts,
 * version product number, and VERSIONED generate must not re-read Registration.
 */
class UltimateSmokeV13RegressionTest {

    private StandardFormatTemplateFactory factory;
    private final LeagueStandardTemplateBuilder league = new LeagueStandardTemplateBuilder();
    private final ArenaStandardTemplateBuilder arena = new ArenaStandardTemplateBuilder();
    private final CupStandardTemplateBuilder cup = new CupStandardTemplateBuilder();
    private final OpenStandardTemplateBuilder open = new OpenStandardTemplateBuilder();

    @BeforeEach
    void setUp() {
        factory = new StandardFormatTemplateFactory(List.of(league, arena, cup, open));
    }

    @Test
    void catalogAlwaysReturnsFourPublicStandards() {
        for (String series : List.of("LEAGUE", "ARENA", "CUP", "OPEN")) {
            Set<String> codes = factory.catalog(series).stream()
                    .map(StandardTemplateDefinition::getTemplateCode)
                    .collect(Collectors.toSet());
            assertEquals(Set.of(
                    StandardTemplateCodes.LEAGUE_STANDARD_V1,
                    StandardTemplateCodes.ARENA_STANDARD_V1,
                    StandardTemplateCodes.CUP_STANDARD_V1,
                    StandardTemplateCodes.OPEN_STANDARD_V1), codes);
        }
    }

    @Test
    void arenaSeriesApplyLeagueStandard_buildsRrThenPlayoff() {
        StandardTemplateDefinition def = factory.builderForTemplateCode(StandardTemplateCodes.LEAGUE_STANDARD_V1)
                .build(teamContext());
        assertEquals(2, def.getStages().size());
        assertEquals(CompetitionFormatEnum.ROUND_ROBIN.getCode(), def.getStages().get(0).getCompetitionFormat());
        assertEquals(CompetitionFormatEnum.SINGLE_ELIMINATION.getCode(), def.getStages().get(1).getCompetitionFormat());
        assertEquals(PairingRuleEnum.HIGH_VS_LOW.getCode(), def.getStages().get(1).getPairingRule());
        assertEquals(1, def.getStages().get(0).getBestOf());
        assertEquals(3, def.getStages().get(1).getBestOf());
        assertEquals(5, def.getStages().get(1).getSemifinalBestOf());
        assertEquals(7, def.getStages().get(1).getFinalBestOf());
    }

    @Test
    void leagueSeriesApplyArenaStandard_buildsSingleElimination() {
        StandardTemplateDefinition def = factory.builderForTemplateCode(StandardTemplateCodes.ARENA_STANDARD_V1)
                .build(teamContext());
        assertEquals(1, def.getStages().size());
        assertEquals(CompetitionFormatEnum.SINGLE_ELIMINATION.getCode(), def.getStages().get(0).getCompetitionFormat());
        assertEquals(3, def.getStages().get(0).getBestOf());
        assertEquals(5, def.getStages().get(0).getSemifinalBestOf());
        assertEquals(5, def.getStages().get(0).getFinalBestOf());
    }

    @Test
    void cupSeriesApplyOpenStandard_buildsSingleElimination() {
        StandardTemplateDefinition def = factory.builderForTemplateCode(StandardTemplateCodes.OPEN_STANDARD_V1)
                .build(teamContext());
        assertEquals(1, def.getStages().size());
        assertEquals(CompetitionFormatEnum.SINGLE_ELIMINATION.getCode(), def.getStages().get(0).getCompetitionFormat());
    }

    @Test
    void openSeriesApplyCupStandard_buildsGroupThenElimination() {
        StandardTemplateDefinition def = factory.builderForTemplateCode(StandardTemplateCodes.CUP_STANDARD_V1)
                .build(teamContext());
        assertEquals(2, def.getStages().size());
        assertEquals(CompetitionFormatEnum.ROUND_ROBIN.getCode(), def.getStages().get(0).getCompetitionFormat());
        assertEquals(CompetitionFormatEnum.SINGLE_ELIMINATION.getCode(), def.getStages().get(1).getCompetitionFormat());
        assertEquals(PairingRuleEnum.GROUP_CROSS.getCode(), def.getStages().get(1).getPairingRule());
    }

    @Test
    void arena16_has15ActualSeriesAndNoBye() {
        EliminationCapacity cap = EliminationCapacity.of(16);
        assertEquals(16, cap.getCapacity());
        assertEquals(0, cap.getByeCount());
        assertEquals(8, cap.getFirstRoundActual());
        assertEquals(15, cap.getTotalActualSeries());
    }

    @Test
    void arena10_hasCapacity16Bye6And9ActualSeries() {
        EliminationCapacity cap = EliminationCapacity.of(10);
        assertEquals(16, cap.getCapacity());
        assertEquals(6, cap.getByeCount());
        assertEquals(2, cap.getFirstRoundActual());
        assertEquals(9, cap.getTotalActualSeries());
    }

    @Test
    void league8_singleRoundRobin28_double56_playoff7() {
        List<Long> teams = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L);
        assertEquals(28, BasketV4RoundRobinScheduler.countMatches(
                BasketV4RoundRobinScheduler.generateRoundRobin(teams, 1)));
        assertEquals(56, BasketV4RoundRobinScheduler.countMatches(
                BasketV4RoundRobinScheduler.generateRoundRobin(teams, 2)));
        assertEquals(7, EliminationCapacity.of(8).getTotalActualSeries());
    }

    @Test
    void cup8_fourGroupsTop2_groupCrossPairs() {
        GroupCrossPlacementStrategy strategy = new GroupCrossPlacementStrategy();
        var plan = strategy.place(GroupCrossPlacementRequest.builder()
                .sourceStageId(100L)
                .advancePerGroup(2)
                .groups(List.of(
                        new GroupLayout(1L, "A", 1),
                        new GroupLayout(2L, "B", 2),
                        new GroupLayout(3L, "C", 3),
                        new GroupLayout(4L, "D", 4)))
                .build());
        List<String> codes = plan.getSlots().stream().map(s -> s.getDisplayCode()).toList();
        assertEquals(List.of("A1", "D2", "A2", "D1", "B1", "C2", "B2", "C1"), codes);
        assertTrue(plan.getSlots().stream().allMatch(s -> s.getEventTeamId() == null));
    }

    @Test
    void lockedDraftRevisionIsProductRuleV1() {
        BasketDivisionFormatSnapshot snapshot = new BasketDivisionFormatSnapshot();
        snapshot.setId(268L);
        snapshot.setSnapshotVersion(3);
        snapshot.setSnapshotStatus(SnapshotStatusEnum.LOCKED.getCode());
        RuleVersionView view = RuleVersionSupport.fromSnapshot(snapshot);
        assertEquals(268L, view.getRuleVersionId());
        assertEquals(3, snapshot.getSnapshotVersion());
        assertEquals(1, view.getRuleVersionNo());
        assertNull(RuleVersionSupport.productRuleVersionNo(draftSnapshot()));
    }

    @Test
    void versionedGenerateMessagesAreSpecificAndNotLegacyRegistration() {
        assertNotEquals(StageSchedulePrerequisiteValidator.MSG_REGISTRATION_NOT_READY,
                StructureGenerationPrerequisiteValidator.MSG_RULE_NOT_LOCKED);
        assertNotEquals(StageSchedulePrerequisiteValidator.MSG_REGISTRATION_NOT_READY,
                StructureGenerationPrerequisiteValidator.MSG_ROSTER_NOT_LOCKED);
        assertFalse(StructureGenerationPrerequisiteValidator.MSG_RULE_NOT_LOCKED.contains("\u62a5\u540d"));
        assertFalse(StructureGenerationPrerequisiteValidator.MSG_ROSTER_NOT_LOCKED.contains("\u62a5\u540d"));
        assertFalse(StructureGenerationPrerequisiteValidator.MSG_FORMAL_INSUFFICIENT.contains("\u62a5\u540d"));
    }

    @Test
    void openTeamAndArenaPersonal_areIndependentOfSeries() {
        assertEquals(RegistrationModeEnum.TEAM.getCode(),
                open.build(teamContext()).getRegistrationMode());
        assertEquals(RegistrationModeEnum.PERSONAL.getCode(),
                arena.build(personalContext()).getRegistrationMode());
        assertEquals(RegistrationModeEnum.PERSONAL.getCode(),
                league.build(personalContext()).getRegistrationMode());
    }

    @Test
    void dirtyFormatTemplateIdCannotOverrideCurrentSnapshot() {
        BasketDivisionFormatSnapshot current = new BasketDivisionFormatSnapshot();
        current.setSourceTemplateId(6L);
        DivisionRuleMutableSpecification spec = new DivisionRuleMutableSpecification();
        assertEquals(6L, spec.resolveFormatTemplateIdForUpdate(3L, current));
    }

    @Test
    void twoDivisionsCapacityMathIsIsolated() {
        EliminationCapacity a = EliminationCapacity.of(16);
        EliminationCapacity b = EliminationCapacity.of(10);
        assertEquals(15, a.getTotalActualSeries());
        assertEquals(9, b.getTotalActualSeries());
        assertEquals(0, a.getByeCount());
        assertEquals(6, b.getByeCount());
    }

    private static StandardTemplateContext teamContext() {
        StandardTemplateContext context = new StandardTemplateContext();
        context.setRegistrationMode(RegistrationModeEnum.TEAM.getCode());
        return context;
    }

    private static StandardTemplateContext personalContext() {
        StandardTemplateContext context = new StandardTemplateContext();
        context.setRegistrationMode(RegistrationModeEnum.PERSONAL.getCode());
        return context;
    }

    private static BasketDivisionFormatSnapshot draftSnapshot() {
        BasketDivisionFormatSnapshot snapshot = new BasketDivisionFormatSnapshot();
        snapshot.setId(1L);
        snapshot.setSnapshotVersion(4);
        snapshot.setSnapshotStatus(SnapshotStatusEnum.DRAFT.getCode());
        return snapshot;
    }
}
