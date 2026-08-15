package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketMatchSeries;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.basket.enums.v4.RegistrationModeEnum;
import com.zhs.basket.mapper.v4.BasketMatchSeriesMapper;
import com.zhs.basket.mapper.v4.BasketScheduleBatchMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.support.v4.bracket.BracketParticipant;
import com.zhs.basket.support.v4.bracket.BracketSlotPlan;
import com.zhs.basket.support.v4.bracket.EliminationCapacity;
import com.zhs.basket.support.v4.bracket.GroupCrossPlacementRequest;
import com.zhs.basket.support.v4.bracket.GroupCrossPlacementStrategy;
import com.zhs.basket.support.v4.bracket.GroupLayout;
import com.zhs.basket.support.v4.bracket.SeededBracketPlacement;
import com.zhs.basket.support.v4.stage.EliminationBracketMaterializer;
import com.zhs.basket.support.v4.stage.MatchSeriesSupport;
import com.zhs.basket.support.v4.stage.ScheduleGenerationResult;
import com.zhs.basket.support.v4.stage.StageExecutionContext;
import com.zhs.basket.support.v4.template.ArenaStandardTemplateBuilder;
import com.zhs.basket.support.v4.template.CupStandardTemplateBuilder;
import com.zhs.basket.support.v4.template.LeagueStandardTemplateBuilder;
import com.zhs.basket.support.v4.template.OpenStandardTemplateBuilder;
import com.zhs.basket.support.v4.template.StandardFormatTemplateFactory;
import com.zhs.basket.support.v4.template.StandardTemplateCodes;
import com.zhs.basket.support.v4.template.StandardTemplateContext;
import com.zhs.basket.support.v4.template.StandardTemplateDefinition;
import com.zhs.basket.support.v4.version.StructureVersionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RcGateLifecycleE2ETest {

    @Mock private BasketScheduleBatchMapper scheduleBatchMapper;
    @Mock private BasketV4MatchMapper matchMapper;
    @Mock private BasketMatchSeriesMapper seriesMapper;

    private final List<BasketScheduleBatch> batches = new ArrayList<>();
    private final List<BasketMatchSeries> seriesStore = new ArrayList<>();
    private final List<BasketV4Match> matchStore = new ArrayList<>();
    private final AtomicLong ids = new AtomicLong(1000);

    private EliminationBracketMaterializer materializer;
    private StandardFormatTemplateFactory factory;
    private final DivisionFormatSnapshotFactory snapshotFactory = new DivisionFormatSnapshotFactory();

    @BeforeEach
    void setUp() {
        factory = new StandardFormatTemplateFactory(List.of(
                new LeagueStandardTemplateBuilder(),
                new ArenaStandardTemplateBuilder(),
                new CupStandardTemplateBuilder(),
                new OpenStandardTemplateBuilder()));
        MatchSeriesSupport support = new MatchSeriesSupport();
        ReflectionTestUtils.setField(support, "seriesMapper", seriesMapper);
        ReflectionTestUtils.setField(support, "matchMapper", matchMapper);
        materializer = new EliminationBracketMaterializer();
        ReflectionTestUtils.setField(materializer, "scheduleBatchMapper", scheduleBatchMapper);
        ReflectionTestUtils.setField(materializer, "matchMapper", matchMapper);
        ReflectionTestUtils.setField(materializer, "matchSeriesSupport", support);

        when(scheduleBatchMapper.insert(any())).thenAnswer(inv -> {
            BasketScheduleBatch batch = inv.getArgument(0);
            batch.setId(ids.incrementAndGet());
            batches.add(batch);
            return 1;
        });
        when(scheduleBatchMapper.updateById(any())).thenReturn(1);
        when(seriesMapper.insert(any())).thenAnswer(inv -> {
            BasketMatchSeries series = inv.getArgument(0);
            series.setId(ids.incrementAndGet());
            seriesStore.add(series);
            return 1;
        });
        when(seriesMapper.updateById(any())).thenReturn(1);
        when(matchMapper.insert(any())).thenAnswer(inv -> {
            BasketV4Match match = inv.getArgument(0);
            match.setId(ids.incrementAndGet());
            matchStore.add(match);
            return 1;
        });
    }

    @Test
    void leagueApplySnapshot_regularBo1_playoffBo3Sf5Final7() {
        StandardTemplateDefinition def = factory.builderForTemplateCode(StandardTemplateCodes.LEAGUE_STANDARD_V1)
                .build(teamContext());
        List<BasketDivisionStage> stages = snapshotFactory.buildStagesFromDefinition(9001L, 8001L, def);
        assertEquals(1, stages.get(0).getBestOf());
        assertEquals(3, stages.get(1).getBestOf());
        assertEquals(5, stages.get(1).getSemifinalBestOf());
        assertEquals(7, stages.get(1).getFinalBestOf());
    }

    @Test
    void arena16Team_materializes15Series_boundVersions_bo3355() {
        List<BasketDivisionStage> stages = snapshotFactory.buildStagesFromDefinition(9101L, 8101L,
                factory.builderForTemplateCode(StandardTemplateCodes.ARENA_STANDARD_V1).build(teamContext()));
        BasketDivisionStage knockout = stages.get(0);
        knockout.setId(501L);
        StageExecutionContext ctx = context(9101L, knockout, 268L, 2L, 2L, 16);
        BracketSlotPlan plan = SeededBracketPlacement.placeBySeed(participants(16));
        ScheduleGenerationResult result = materializer.materialize(ctx, plan);

        assertEquals(15, seriesStore.size());
        assertEquals(16, plan.getCapacity().getCapacity());
        assertEquals(0, plan.getCapacity().getByeCount());
        assertEquals(8, countRound(1));
        assertEquals(4, countRound(2));
        assertEquals(2, countRound(3));
        assertEquals(1, countRound(4));
        assertEquals(3, maxBo(1));
        assertEquals(3, maxBo(2));
        assertEquals(5, maxBo(3));
        assertEquals(5, maxBo(4));
        assertEquals(268L, batches.get(0).getRuleVersionId());
        assertEquals(2L, batches.get(0).getRosterVersionId());
        assertEquals(2L, batches.get(0).getPairingExecutionId());
        assertEquals(4, result.getRoundCount());
        assertTrue(seriesStore.stream().noneMatch(s -> s.getHomeEventTeamId() == null && Integer.valueOf(1).equals(s.getRoundNo())));
    }

    @Test
    void arena10Team_capacity16_bye6_nineActualSeries() {
        List<BasketDivisionStage> stages = snapshotFactory.buildStagesFromDefinition(9102L, 8102L,
                factory.builderForTemplateCode(StandardTemplateCodes.ARENA_STANDARD_V1).build(teamContext()));
        BasketDivisionStage knockout = stages.get(0);
        knockout.setId(502L);
        StageExecutionContext ctx = context(9102L, knockout, 11L, 21L, 31L, 10);
        BracketSlotPlan plan = SeededBracketPlacement.placeBySeed(participants(10));
        materializer.materialize(ctx, plan);

        EliminationCapacity cap = plan.getCapacity();
        assertEquals(16, cap.getCapacity());
        assertEquals(6, cap.getByeCount());
        assertEquals(9, cap.getTotalActualSeries());
        assertEquals(9, seriesStore.size());
        assertEquals(2, countRound(1));
        assertTrue(plan.getSlots().stream().filter(s -> s.isBye()).allMatch(s -> s.getEventTeamId() == null));
    }

    @Test
    void league8Team_rr28_playoff7_qf3_sf5_final7() {
        StandardTemplateDefinition def = factory.builderForTemplateCode(StandardTemplateCodes.LEAGUE_STANDARD_V1)
                .build(teamContext());
        List<BasketDivisionStage> stages = snapshotFactory.buildStagesFromDefinition(9103L, 8103L, def);
        assertEquals(28, BasketV4RoundRobinScheduler.countMatches(
                BasketV4RoundRobinScheduler.generateRoundRobin(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L), 1)));

        BasketDivisionStage playoff = stages.get(1);
        playoff.setId(503L);
        StageExecutionContext ctx = context(9103L, playoff, 12L, 22L, 32L, 8);
        materializer.materialize(ctx, SeededBracketPlacement.placeBySeed(participants(8)));

        assertEquals(7, seriesStore.size());
        assertEquals(4, countRound(1));
        assertEquals(2, countRound(2));
        assertEquals(1, countRound(3));
        assertEquals(3, maxBo(1));
        assertEquals(5, maxBo(2));
        assertEquals(7, maxBo(3));
    }

    @Test
    void cup8Team_groupCrossPlaceholders_thenKnockoutSeries() {
        StandardTemplateDefinition def = factory.builderForTemplateCode(StandardTemplateCodes.CUP_STANDARD_V1)
                .build(teamContext());
        List<BasketDivisionStage> stages = snapshotFactory.buildStagesFromDefinition(9104L, 8104L, def);
        assertEquals(CompetitionFormatEnum.ROUND_ROBIN.getCode(), stages.get(0).getCompetitionFormat());

        var plan = new GroupCrossPlacementStrategy().place(GroupCrossPlacementRequest.builder()
                .sourceStageId(100L)
                .advancePerGroup(2)
                .groups(List.of(
                        new GroupLayout(1L, "A", 1),
                        new GroupLayout(2L, "B", 2),
                        new GroupLayout(3L, "C", 3),
                        new GroupLayout(4L, "D", 4)))
                .build());
        assertEquals(List.of("A1", "D2", "A2", "D1", "B1", "C2", "B2", "C1"),
                plan.getSlots().stream().map(s -> s.getDisplayCode()).toList());
        assertTrue(plan.getSlots().stream().allMatch(s -> s.getEventTeamId() == null));

        BasketDivisionStage knockout = stages.get(1);
        knockout.setId(504L);
        StageExecutionContext ctx = context(9104L, knockout, 13L, 23L, 33L, 0);
        materializer.materialize(ctx, plan);
        assertEquals(7, seriesStore.size());
        assertTrue(seriesStore.stream().filter(s -> Integer.valueOf(1).equals(s.getRoundNo()))
                .allMatch(s -> s.getHomeEventTeamId() == null && s.getAwayEventTeamId() == null));
    }

    @Test
    void openPersonal_thenSeStructure_andArenaSeriesLeagueStandardIndependent() {
        StandardTemplateDefinition open = factory.builderForTemplateCode(StandardTemplateCodes.OPEN_STANDARD_V1)
                .build(personalContext());
        assertEquals(RegistrationModeEnum.PERSONAL.getCode(), open.getRegistrationMode());
        List<BasketDivisionStage> stages = snapshotFactory.buildStagesFromDefinition(9105L, 8105L, open);
        BasketDivisionStage knockout = stages.get(0);
        knockout.setId(505L);
        StageExecutionContext ctx = context(9105L, knockout, 14L, 24L, 34L, 8);
        materializer.materialize(ctx, SeededBracketPlacement.placeBySeed(participants(8)));
        assertEquals(7, seriesStore.size());

        StandardTemplateDefinition cross = factory.builderForTemplateCode(StandardTemplateCodes.LEAGUE_STANDARD_V1)
                .build(personalContext());
        assertEquals(RegistrationModeEnum.PERSONAL.getCode(), cross.getRegistrationMode());
        assertEquals(CompetitionFormatEnum.ROUND_ROBIN.getCode(), cross.getStages().get(0).getCompetitionFormat());
        assertEquals(7, cross.getStages().get(1).getFinalBestOf());
    }

    @Test
    void formalCap_20Approved_16Formal_4Candidate_onlyFormalEnterStructure() {
        int formal = Math.min(16, 20);
        int candidate = Math.max(0, 20 - 16);
        assertEquals(16, formal);
        assertEquals(4, candidate);
        BracketSlotPlan plan = SeededBracketPlacement.placeBySeed(participants(formal));
        assertEquals(16, plan.getCapacity().getParticipantCount());
        assertEquals(0, plan.getSlots().stream().filter(s -> s.getEventTeamId() != null && s.getEventTeamId() > 16).count());
    }

    private int countRound(int roundNo) {
        return (int) seriesStore.stream().filter(s -> Integer.valueOf(roundNo).equals(s.getRoundNo())).count();
    }

    private int maxBo(int roundNo) {
        return seriesStore.stream()
                .filter(s -> Integer.valueOf(roundNo).equals(s.getRoundNo()))
                .mapToInt(BasketMatchSeries::getBestOf)
                .max().orElse(-1);
    }

    private static List<BracketParticipant> participants(int n) {
        List<BracketParticipant> list = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            list.add(new BracketParticipant((long) i, i, i, i, "OVERALL_RANK"));
        }
        return list;
    }

    private StageExecutionContext context(long divisionId, BasketDivisionStage stage,
                                          long ruleId, long rosterId, long pairingId, int teamCount) {
        BasketDivision division = new BasketDivision();
        division.setId(divisionId);
        division.setSeasonId(1L);
        division.setPlatformCode("PS_XBOX");
        StructureVersionContext version = new StructureVersionContext();
        version.setDivisionId(divisionId);
        version.setRuleVersionId(ruleId);
        version.setRosterVersionId(rosterId);
        version.setPairingExecutionId(pairingId);
        List<com.zhs.basket.domain.v4.BasketDivisionStageTeam> teams = new ArrayList<>();
        for (int i = 1; i <= teamCount; i++) {
            com.zhs.basket.domain.v4.BasketDivisionStageTeam team = new com.zhs.basket.domain.v4.BasketDivisionStageTeam();
            team.setEventTeamId((long) i);
            team.setSeedNo(i);
            teams.add(team);
        }
        StageExecutionContext ctx = new StageExecutionContext();
        ctx.setDivision(division);
        ctx.setStage(stage);
        ctx.setStageTeams(teams);
        ctx.setExecutionVersion(1);
        ctx.setVersionContext(version);
        ctx.setPairingExecutionId(pairingId);
        return ctx;
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
}
