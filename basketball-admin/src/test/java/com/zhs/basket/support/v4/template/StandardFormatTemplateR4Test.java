package com.zhs.basket.support.v4.template;

import com.zhs.basket.domain.v4.BasketDivisionFormatSnapshot;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketFormatTemplate;
import com.zhs.basket.enums.v4.AdvancementTypeEnum;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.basket.enums.v4.PairingRuleEnum;
import com.zhs.basket.enums.v4.RegistrationModeEnum;
import com.zhs.basket.enums.v4.RosterWorkflowModeEnum;
import com.zhs.basket.enums.v4.SeriesCodeEnum;
import com.zhs.basket.enums.v4.TemplateTypeEnum;
import com.zhs.basket.support.v4.DivisionFormatSnapshotFactory;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandardFormatTemplateR4Test {

    private StandardFormatTemplateFactory factory;
    private final LeagueStandardTemplateBuilder league = new LeagueStandardTemplateBuilder();
    private final ArenaStandardTemplateBuilder arena = new ArenaStandardTemplateBuilder();
    private final CupStandardTemplateBuilder cup = new CupStandardTemplateBuilder();
    private final OpenStandardTemplateBuilder open = new OpenStandardTemplateBuilder();
    private final StandardTemplateGraphGuard guard = new StandardTemplateGraphGuard();
    private final DivisionFormatSnapshotFactory snapshotFactory = new DivisionFormatSnapshotFactory();

    @BeforeEach
    void setUp() {
        factory = new StandardFormatTemplateFactory(List.of(league, arena, cup, open));
    }

    @Test
    void test1_fourStandardTemplatesLookup() {
        assertEquals(StandardTemplateCodes.LEAGUE_STANDARD_V1, factory.builderForSeries("LEAGUE").templateCode());
        assertEquals(StandardTemplateCodes.ARENA_STANDARD_V1, factory.builderForSeries("ARENA").templateCode());
        assertEquals(StandardTemplateCodes.CUP_STANDARD_V1, factory.builderForSeries("CUP").templateCode());
        assertEquals(StandardTemplateCodes.OPEN_STANDARD_V1, factory.builderForSeries("OPEN").templateCode());
        assertEquals(4, factory.catalog("LEAGUE").size());
    }

    @Test
    void test2_customTemplateCodeNotStandard() {
        assertFalse(StandardTemplateCodes.isStandard("ROUND_ROBIN_BASIC_V1"));
        assertFalse(StandardTemplateCodes.isStandard("GROUP_STAGE_BASIC_V1"));
        assertFalse(StandardTemplateCodes.isStandard("SINGLE_ELIM_BO1_V1"));
    }

    @Test
    void test3_leagueStageGraphRrThenPlayoff() {
        StandardTemplateDefinition def = league.build(context(RegistrationModeEnum.TEAM.getCode()));
        assertEquals(2, def.getStages().size());
        assertEquals("REGULAR", def.getStages().get(0).getStageCode());
        assertEquals(CompetitionFormatEnum.ROUND_ROBIN.getCode(), def.getStages().get(0).getCompetitionFormat());
        assertEquals("PLAYOFF", def.getStages().get(1).getStageCode());
        assertEquals(CompetitionFormatEnum.SINGLE_ELIMINATION.getCode(), def.getStages().get(1).getCompetitionFormat());
        assertEquals(AdvancementTypeEnum.OVERALL_TOP_N.getCode(), def.getStages().get(0).getAdvancementType());
        assertEquals(1, def.getStages().get(0).getBestOf());
        assertEquals(3, def.getStages().get(1).getBestOf());
        assertEquals(5, def.getStages().get(1).getSemifinalBestOf());
        assertEquals(7, def.getStages().get(1).getFinalBestOf());
    }

    @Test
    void test4_leagueCyclesTwoSaved() {
        StandardTemplateContext context = context(RegistrationModeEnum.TEAM.getCode());
        context.setRoundRobinCycles(2);
        StandardTemplateDefinition def = league.build(context);
        assertEquals(2, def.getStages().get(0).getRoundRobinCycles());
    }

    @Test
    void test5_leaguePlayoffSlotsSixteenNotHardcodedEight() {
        StandardTemplateContext context = context(RegistrationModeEnum.TEAM.getCode());
        context.setPlayoffSlots(16);
        StandardTemplateDefinition def = league.build(context);
        assertEquals(16, def.getStages().get(0).getAdvancementCount());
        assertFalse(def.getStages().stream().allMatch(s -> Integer.valueOf(8).equals(s.getAdvancementCount())));
    }

    @Test
    void test6_leaguePairingHighVsLow() {
        StandardTemplateDefinition def = league.build(context(RegistrationModeEnum.TEAM.getCode()));
        assertEquals(PairingRuleEnum.HIGH_VS_LOW.getCode(), def.getStages().get(1).getPairingRule());
    }

    @Test
    void test7_leaguePersonalCanCreateDraft() {
        StandardTemplateDefinition def = league.build(context(RegistrationModeEnum.PERSONAL.getCode()));
        assertEquals(RegistrationModeEnum.PERSONAL.getCode(), def.getRegistrationMode());
        assertEquals(RegistrationModeEnum.TEAM.getCode(), def.getDefaultRegistrationMode());
    }

    @Test
    void test8_arenaOnlyElimination() {
        StandardTemplateDefinition def = arena.build(context(RegistrationModeEnum.TEAM.getCode()));
        assertEquals(1, def.getStages().size());
        assertEquals("KNOCKOUT", def.getStages().get(0).getStageCode());
        assertEquals(CompetitionFormatEnum.SINGLE_ELIMINATION.getCode(), def.getStages().get(0).getCompetitionFormat());
        assertEquals(3, def.getStages().get(0).getBestOf());
        assertEquals(5, def.getStages().get(0).getSemifinalBestOf());
        assertEquals(5, def.getStages().get(0).getFinalBestOf());
        assertTrue(def.getStages().stream().noneMatch(s -> "GROUP".equals(s.getStageType())));
    }

    @Test
    void test9_arenaPersonalCanCreate() {
        StandardTemplateDefinition def = arena.build(context(RegistrationModeEnum.PERSONAL.getCode()));
        assertEquals(RegistrationModeEnum.PERSONAL.getCode(), def.getRegistrationMode());
    }

    @Test
    void test10_cupGroupThenElimination() {
        StandardTemplateDefinition def = cup.build(context(RegistrationModeEnum.TEAM.getCode()));
        assertEquals(2, def.getStages().size());
        assertEquals("GROUP", def.getStages().get(0).getStageCode());
        assertEquals(CompetitionFormatEnum.ROUND_ROBIN.getCode(), def.getStages().get(0).getCompetitionFormat());
        assertEquals("KNOCKOUT", def.getStages().get(1).getStageCode());
        assertEquals(CompetitionFormatEnum.SINGLE_ELIMINATION.getCode(), def.getStages().get(1).getCompetitionFormat());
        assertEquals(AdvancementTypeEnum.GROUP_TOP_N.getCode(), def.getStages().get(0).getAdvancementType());
        assertEquals(5, def.getStages().get(1).getSemifinalBestOf());
        assertEquals(5, def.getStages().get(1).getFinalBestOf());
        assertTrue(def.getOptions().stream().anyMatch(o -> "semifinalBestOf".equals(o.getKey())));
        assertTrue(def.getOptions().stream().anyMatch(o -> "finalBestOf".equals(o.getKey())));
    }

    @Test
    void test11_cupGroupCyclesAndAdvancePerGroupSaved() {
        StandardTemplateContext context = context(RegistrationModeEnum.TEAM.getCode());
        context.setGroupCycles(2);
        context.setAdvancePerGroup(3);
        context.setGroupCount(4);
        StandardTemplateDefinition def = cup.build(context);
        assertEquals(2, def.getStages().get(0).getRoundRobinCycles());
        assertEquals(3, def.getStages().get(0).getAdvancementCount());
    }

    @Test
    void test12_cupPlacementGroupCross() {
        StandardTemplateDefinition def = cup.build(context(RegistrationModeEnum.TEAM.getCode()));
        assertEquals(PairingRuleEnum.GROUP_CROSS.getCode(), def.getStages().get(1).getPairingRule());
        assertEquals(AdvancementTypeEnum.GROUP_TOP_N.getCode(), def.getStages().get(0).getAdvancementType());
    }

    @Test
    void test13_cupPersonalCanCreate() {
        StandardTemplateDefinition def = cup.build(context(RegistrationModeEnum.PERSONAL.getCode()));
        assertEquals(RegistrationModeEnum.PERSONAL.getCode(), def.getRegistrationMode());
    }

    @Test
    void test14_openOnlyCompetitionEliminationNoFormationStage() {
        StandardTemplateDefinition def = open.build(context(RegistrationModeEnum.PERSONAL.getCode()));
        assertEquals(1, def.getStages().size());
        assertEquals("KNOCKOUT", def.getStages().get(0).getStageCode());
        assertTrue(def.getStages().stream().noneMatch(s ->
                s.getStageCode().contains("FORMATION") || "FORMATION".equals(s.getStageType())));
        assertTrue(def.getFormationNote() != null && !def.getFormationNote().isEmpty());
        assertEquals(5, def.getStages().get(0).getSemifinalBestOf());
        assertEquals(5, def.getStages().get(0).getFinalBestOf());
        assertTrue(def.getOptions().stream().anyMatch(o -> "semifinalBestOf".equals(o.getKey())));
        assertTrue(def.getOptions().stream().anyMatch(o -> "finalBestOf".equals(o.getKey())));
    }

    @Test
    void test15_openTeamCanCreate() {
        StandardTemplateDefinition def = open.build(context(RegistrationModeEnum.TEAM.getCode()));
        assertEquals(RegistrationModeEnum.TEAM.getCode(), def.getRegistrationMode());
        assertEquals(RegistrationModeEnum.PERSONAL.getCode(), def.getDefaultRegistrationMode());
    }

    @Test
    void test16_standardRejectsCoreStageDelete() {
        BasketDivisionFormatSnapshot snapshot = new BasketDivisionFormatSnapshot();
        snapshot.setSourceTemplateType(TemplateTypeEnum.STANDARD.getCode());
        snapshot.setSourceTemplateCode(StandardTemplateCodes.LEAGUE_STANDARD_V1);
        ServiceException ex = assertThrows(ServiceException.class, () -> guard.assertCanAddOrDeleteOrReorder(snapshot));
        assertTrue(ex.getMessage().contains("STANDARD"));
    }

    @Test
    void test17_customAllowsFreeStageGraph() {
        BasketDivisionFormatSnapshot snapshot = new BasketDivisionFormatSnapshot();
        snapshot.setSourceTemplateType(TemplateTypeEnum.CUSTOM.getCode());
        snapshot.setSourceTemplateCode("ROUND_ROBIN_BASIC_V1");
        guard.assertCanAddOrDeleteOrReorder(snapshot);
        assertFalse(guard.isStandard(snapshot));
    }

    @Test
    void test18_lockedSnapshotValuesCopiedNotLiveTemplate() {
        StandardTemplateContext context = context(RegistrationModeEnum.TEAM.getCode());
        context.setPlayoffSlots(8);
        StandardTemplateDefinition first = league.build(context);
        List<BasketDivisionStage> stages = snapshotFactory.buildStagesFromDefinition(1L, 2L, first);
        context.setPlayoffSlots(16);
        StandardTemplateDefinition second = league.build(context);
        assertEquals(8, stages.get(0).getAdvancementCount());
        assertEquals(16, second.getStages().get(0).getAdvancementCount());
    }

    @Test
    void test19_seriesDefaultTemplate() {
        assertEquals(StandardTemplateCodes.LEAGUE_STANDARD_V1, factory.defaultTemplateCode("LEAGUE"));
        assertEquals(StandardTemplateCodes.ARENA_STANDARD_V1, factory.defaultTemplateCode("ARENA"));
        assertEquals(StandardTemplateCodes.CUP_STANDARD_V1, factory.defaultTemplateCode("CUP"));
        assertEquals(StandardTemplateCodes.OPEN_STANDARD_V1, factory.defaultTemplateCode("OPEN"));
    }

    @Test
    void test20_registrationModeNotHardBoundToSeries() {
        assertEquals(RegistrationModeEnum.PERSONAL.getCode(),
                league.build(context(RegistrationModeEnum.PERSONAL.getCode())).getRegistrationMode());
        assertEquals(RegistrationModeEnum.PERSONAL.getCode(),
                arena.build(context(RegistrationModeEnum.PERSONAL.getCode())).getRegistrationMode());
        assertEquals(RegistrationModeEnum.PERSONAL.getCode(),
                cup.build(context(RegistrationModeEnum.PERSONAL.getCode())).getRegistrationMode());
        assertEquals(RegistrationModeEnum.TEAM.getCode(),
                open.build(context(RegistrationModeEnum.TEAM.getCode())).getRegistrationMode());
    }

    @Test
    void test23_publicCatalogReturnsAllFourForAnySeries() {
        for (String seriesCode : List.of("LEAGUE", "ARENA", "CUP", "OPEN")) {
            var codes = factory.catalog(seriesCode).stream()
                    .map(StandardTemplateDefinition::getTemplateCode)
                    .collect(java.util.stream.Collectors.toSet());
            assertEquals(4, codes.size());
            assertTrue(codes.contains(StandardTemplateCodes.LEAGUE_STANDARD_V1));
            assertTrue(codes.contains(StandardTemplateCodes.ARENA_STANDARD_V1));
            assertTrue(codes.contains(StandardTemplateCodes.CUP_STANDARD_V1));
            assertTrue(codes.contains(StandardTemplateCodes.OPEN_STANDARD_V1));
        }
    }

    @Test
    void test24_crossSeriesStandardTemplatesResolvable() {
        assertEquals(StandardTemplateCodes.ARENA_STANDARD_V1,
                factory.builderForTemplateCode(StandardTemplateCodes.ARENA_STANDARD_V1).templateCode());
        assertEquals(StandardTemplateCodes.CUP_STANDARD_V1,
                factory.builderForTemplateCode(StandardTemplateCodes.CUP_STANDARD_V1).templateCode());
        assertEquals(StandardTemplateCodes.OPEN_STANDARD_V1,
                factory.builderForTemplateCode(StandardTemplateCodes.OPEN_STANDARD_V1).templateCode());
        assertEquals(StandardTemplateCodes.LEAGUE_STANDARD_V1,
                factory.builderForTemplateCode(StandardTemplateCodes.LEAGUE_STANDARD_V1).templateCode());
    }

    @Test
    void test25_applyRequestDoesNotCarrySeriesIdentity() {
        var fields = java.util.Arrays.stream(com.zhs.basket.dto.v4.StandardTemplateApplyReq.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)
                .collect(java.util.stream.Collectors.toSet());
        assertFalse(fields.contains("seriesId"));
        assertFalse(fields.contains("seriesCode"));
        assertFalse(fields.contains("seriesType"));
    }

    @Test
    void test21_newSnapshotHeaderIsVersioned() {
        BasketFormatTemplate template = new BasketFormatTemplate();
        template.setId(1L);
        template.setTemplateCode(StandardTemplateCodes.LEAGUE_STANDARD_V1);
        template.setTemplateName("League");
        template.setVersionNo(1);
        template.setTemplateType(TemplateTypeEnum.STANDARD.getCode());
        var snapshot = snapshotFactory.buildSnapshotHeader(9L, template, 1, null);
        assertEquals(RosterWorkflowModeEnum.VERSIONED.getCode(), snapshot.getRosterWorkflowMode());
        assertEquals(TemplateTypeEnum.STANDARD.getCode(), snapshot.getSourceTemplateType());
    }

    @Test
    void test22_seedSqlDoesNotUpdateSnapshots() throws Exception {
        Path sql = resolveR4Sql();
        String text = Files.readString(sql);
        assertFalse(text.lines().anyMatch(line -> {
            String trimmed = line.trim();
            return trimmed.regionMatches(true, 0, "UPDATE", 0, 6)
                    && trimmed.contains("basket_division_format_snapshot");
        }));
        assertTrue(text.contains("LEAGUE_STANDARD_V1"));
        assertTrue(text.contains("ARENA_STANDARD_V1"));
        assertTrue(text.contains("CUP_STANDARD_V1"));
        assertTrue(text.contains("OPEN_STANDARD_V1"));
    }

    @Test
    void factoryHasNoSeriesEngineBranchingOnGenerators() {
        assertEquals(SeriesCodeEnum.LEAGUE, league.support());
        assertEquals(SeriesCodeEnum.OPEN, open.support());
    }

    private static StandardTemplateContext context(String registrationMode) {
        StandardTemplateContext context = new StandardTemplateContext();
        context.setRegistrationMode(registrationMode);
        return context;
    }

    private static Path resolveR4Sql() throws Exception {
        Path[] candidates = new Path[] {
                Path.of("docs/sql/cnpa-v13-r4-standard-template-migration.sql"),
                Path.of("../docs/sql/cnpa-v13-r4-standard-template-migration.sql"),
                Path.of("../../docs/sql/cnpa-v13-r4-standard-template-migration.sql"),
                Path.of(System.getProperty("user.dir"), "..", "docs", "sql", "cnpa-v13-r4-standard-template-migration.sql")
        };
        for (Path candidate : candidates) {
            Path normalized = candidate.normalize().toAbsolutePath();
            if (Files.exists(normalized)) {
                return normalized;
            }
        }
        throw new IllegalStateException("R4 SQL not found from " + System.getProperty("user.dir"));
    }
}
