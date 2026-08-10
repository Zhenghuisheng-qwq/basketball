package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketDivisionStageTeam;
import com.zhs.basket.domain.v4.BasketMatchSeries;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.enums.v4.AdvancementTypeEnum;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.basket.enums.v4.GroupingModeEnum;
import com.zhs.basket.enums.v4.OrganizationModeEnum;
import com.zhs.basket.enums.v4.PairingRuleEnum;
import com.zhs.basket.enums.v4.StageTypeEnum;
import com.zhs.basket.mapper.v4.BasketMatchSeriesMapper;
import com.zhs.basket.mapper.v4.BasketScheduleBatchMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.support.v4.ByeQualificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RandomDrawPairingRuleTest {

    @Mock
    private BasketScheduleBatchMapper scheduleBatchMapper;
    @Mock
    private BasketV4MatchMapper matchMapper;
    @Mock
    private BasketMatchSeriesMapper seriesMapper;
    @Mock
    private ByeQualificationService byeQualificationService;

    @InjectMocks
    private MatchSeriesEliminationScheduleGenerator generator;

    private SingleEliminationStageScheduleStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new SingleEliminationStageScheduleStrategy();
        ReflectionTestUtils.setField(strategy, "seriesScheduleGenerator", generator);
        ReflectionTestUtils.setField(strategy, "scheduleBatchMapper", scheduleBatchMapper);
        ReflectionTestUtils.setField(strategy, "matchMapper", matchMapper);
        ReflectionTestUtils.setField(strategy, "byeQualificationService", byeQualificationService);
        ReflectionTestUtils.setField(generator, "byeQualificationService", byeQualificationService);
    }

    @Test
    void orderTeams_randomDraw_shufflesCopyWithoutMutatingInput() {
        List<BasketDivisionStageTeam> original = teams(8);
        List<Long> beforeIds = original.stream().map(BasketDivisionStageTeam::getEventTeamId).toList();

        List<BasketDivisionStageTeam> ordered = EliminationByeSupport.orderTeamsForPairing(
                original, PairingRuleEnum.RANDOM_DRAW.getCode(), new Random(42L));

        assertEquals(beforeIds, original.stream().map(BasketDivisionStageTeam::getEventTeamId).toList());
        assertEquals(8, ordered.size());
        assertEquals(Set.copyOf(beforeIds), ordered.stream().map(BasketDivisionStageTeam::getEventTeamId).collect(Collectors.toSet()));
        assertNotEquals(beforeIds, ordered.stream().map(BasketDivisionStageTeam::getEventTeamId).toList());
    }

    @Test
    void orderTeams_highVsLow_keepsSeedOrder() {
        List<BasketDivisionStageTeam> teams = teams(4);
        List<BasketDivisionStageTeam> ordered = EliminationByeSupport.orderTeamsForPairing(
                teams, PairingRuleEnum.HIGH_VS_LOW.getCode(), new Random(1L));
        assertEquals(List.of(1L, 2L, 3L, 4L),
                ordered.stream().map(BasketDivisionStageTeam::getEventTeamId).toList());
    }

    @Test
    void orderTeams_nullOrBlank_defaultsToSeedOrder() {
        List<BasketDivisionStageTeam> teams = teams(4);
        assertEquals(List.of(1L, 2L, 3L, 4L),
                EliminationByeSupport.orderTeamsForPairing(teams, null).stream()
                        .map(BasketDivisionStageTeam::getEventTeamId).toList());
        assertEquals(List.of(1L, 2L, 3L, 4L),
                EliminationByeSupport.orderTeamsForPairing(teams, "").stream()
                        .map(BasketDivisionStageTeam::getEventTeamId).toList());
    }

    @Test
    void case1_eightTeamsBo1Random_fourFirstRoundPairsCoverAllTeamsOnce() {
        StageExecutionContext context = buildBo1Context(8, AdvancementTypeEnum.NONE, null);
        context.getStage().setPairingRule(PairingRuleEnum.RANDOM_DRAW.getCode());
        strategy.random = new Random(7L);
        stubBatchInsert();
        AtomicInteger matchId = new AtomicInteger(100);
        when(matchMapper.insert(any())).thenAnswer(invocation -> {
            BasketV4Match match = invocation.getArgument(0);
            match.setId((long) matchId.getAndIncrement());
            return 1;
        });

        ScheduleGenerationResult result = strategy.generate(context);
        assertEquals(7, result.getExpectedMatchCount());

        ArgumentCaptor<BasketV4Match> captor = ArgumentCaptor.forClass(BasketV4Match.class);
        verify(matchMapper, times(7)).insert(captor.capture());
        List<BasketV4Match> firstRound = captor.getAllValues().stream()
                .filter(m -> m.getRoundNo() != null && m.getRoundNo() == 1)
                .toList();
        assertEquals(4, firstRound.size());
        assertTeamCoverage(firstRound, 8);
    }

    @Test
    void case2_sixteenTeamsRandom_eightFirstRoundAndFifteenTotal() {
        StageExecutionContext context = buildBo1Context(16, AdvancementTypeEnum.NONE, null);
        context.getStage().setPairingRule(PairingRuleEnum.RANDOM_DRAW.getCode());
        strategy.random = new Random(11L);
        stubBatchInsert();
        AtomicInteger matchId = new AtomicInteger(200);
        when(matchMapper.insert(any())).thenAnswer(invocation -> {
            BasketV4Match match = invocation.getArgument(0);
            match.setId((long) matchId.getAndIncrement());
            return 1;
        });

        ScheduleGenerationResult result = strategy.generate(context);
        assertEquals(15, result.getExpectedMatchCount());

        ArgumentCaptor<BasketV4Match> captor = ArgumentCaptor.forClass(BasketV4Match.class);
        verify(matchMapper, times(15)).insert(captor.capture());
        List<BasketV4Match> firstRound = captor.getAllValues().stream()
                .filter(m -> m.getRoundNo() != null && m.getRoundNo() == 1)
                .toList();
        assertEquals(8, firstRound.size());
        assertTeamCoverage(firstRound, 16);
    }

    @Test
    void case3_fifteenTeamsRandom_byeReuse_noDuplicateOrMissing() {
        StageExecutionContext context = buildBo1Context(15, AdvancementTypeEnum.WINNER_ADVANCE, "SF");
        context.getStage().setPairingRule(PairingRuleEnum.RANDOM_DRAW.getCode());
        strategy.random = new Random(99L);
        stubBatchInsert();
        when(matchMapper.insert(any())).thenReturn(1);

        ScheduleGenerationResult result = strategy.generate(context);
        assertEquals(7, result.getExpectedMatchCount());

        ArgumentCaptor<BasketV4Match> captor = ArgumentCaptor.forClass(BasketV4Match.class);
        verify(matchMapper, times(7)).insert(captor.capture());
        assertTeamCoverage(captor.getAllValues(), 14);
        verify(byeQualificationService, times(1)).upsertByePreview(any(), any());
    }

    @Test
    void case4_randomDrawBo3_seriesBestOf3() {
        StageExecutionContext context = buildSeriesContext(8, 3, AdvancementTypeEnum.WINNER_ADVANCE, "FINAL");
        context.getStage().setPairingRule(PairingRuleEnum.RANDOM_DRAW.getCode());
        strategy.random = new Random(3L);
        stubBatchInsert();
        stubSeriesInsertFrom(1L);
        when(matchMapper.insert(any())).thenReturn(1);

        ScheduleGenerationResult result = strategy.generate(context);
        assertEquals(12, result.getExpectedMatchCount());

        ArgumentCaptor<BasketMatchSeries> seriesCaptor = ArgumentCaptor.forClass(BasketMatchSeries.class);
        verify(seriesMapper, times(4)).insert(seriesCaptor.capture());
        for (BasketMatchSeries series : seriesCaptor.getAllValues()) {
            assertEquals(3, series.getBestOf());
        }
        ArgumentCaptor<BasketV4Match> matchCaptor = ArgumentCaptor.forClass(BasketV4Match.class);
        verify(matchMapper, times(12)).insert(matchCaptor.capture());
        Set<Long> teams = new HashSet<>();
        for (BasketV4Match m : matchCaptor.getAllValues()) {
            if (Integer.valueOf(1).equals(m.getSeriesGameNo())) {
                teams.add(m.getHomeEventTeamId());
                teams.add(m.getAwayEventTeamId());
            }
        }
        assertEquals(8, teams.size());
    }

    @Test
    void case5_randomDrawBo5_seriesBestOf5() {
        StageExecutionContext context = buildSeriesContext(4, 5, AdvancementTypeEnum.WINNER_ADVANCE, "FINAL");
        context.getStage().setPairingRule(PairingRuleEnum.RANDOM_DRAW.getCode());
        strategy.random = new Random(5L);
        stubBatchInsert();
        stubSeriesInsertFrom(10L);
        when(matchMapper.insert(any())).thenReturn(1);

        ScheduleGenerationResult result = strategy.generate(context);
        assertEquals(10, result.getExpectedMatchCount());

        ArgumentCaptor<BasketMatchSeries> seriesCaptor = ArgumentCaptor.forClass(BasketMatchSeries.class);
        verify(seriesMapper, times(2)).insert(seriesCaptor.capture());
        for (BasketMatchSeries series : seriesCaptor.getAllValues()) {
            assertEquals(5, series.getBestOf());
        }
    }

    @Test
    void case6_onlyEligibleStageTeamsArePaired() {
        StageExecutionContext context = buildBo1Context(8, AdvancementTypeEnum.NONE, null);
        context.getStage().setPairingRule(PairingRuleEnum.RANDOM_DRAW.getCode());
        // 模拟晋级 8 队（非 division 全量）：仅使用 stageTeams
        strategy.random = new Random(21L);
        stubBatchInsert();
        AtomicInteger matchId = new AtomicInteger(300);
        when(matchMapper.insert(any())).thenAnswer(invocation -> {
            BasketV4Match match = invocation.getArgument(0);
            match.setId((long) matchId.getAndIncrement());
            return 1;
        });

        strategy.generate(context);

        ArgumentCaptor<BasketV4Match> captor = ArgumentCaptor.forClass(BasketV4Match.class);
        verify(matchMapper, times(7)).insert(captor.capture());
        Set<Long> firstRoundTeams = new HashSet<>();
        for (BasketV4Match m : captor.getAllValues()) {
            if (m.getRoundNo() != null && m.getRoundNo() == 1) {
                firstRoundTeams.add(m.getHomeEventTeamId());
                firstRoundTeams.add(m.getAwayEventTeamId());
            }
        }
        assertEquals(Set.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L), firstRoundTeams);
    }

    @Test
    void case8_sameSeedProducesSamePairings_twice() {
        List<BasketDivisionStageTeam> teams = teams(8);
        List<Long> order1 = EliminationByeSupport.orderTeamsForPairing(
                teams, PairingRuleEnum.RANDOM_DRAW.getCode(), new Random(12345L)).stream()
                .map(BasketDivisionStageTeam::getEventTeamId).toList();
        List<Long> order2 = EliminationByeSupport.orderTeamsForPairing(
                teams, PairingRuleEnum.RANDOM_DRAW.getCode(), new Random(12345L)).stream()
                .map(BasketDivisionStageTeam::getEventTeamId).toList();
        assertEquals(order1, order2);

        StageExecutionContext ctx1 = buildBo1Context(8, AdvancementTypeEnum.WINNER_ADVANCE, "SF");
        ctx1.getStage().setPairingRule(PairingRuleEnum.RANDOM_DRAW.getCode());
        StageExecutionContext ctx2 = buildBo1Context(8, AdvancementTypeEnum.WINNER_ADVANCE, "SF");
        ctx2.getStage().setPairingRule(PairingRuleEnum.RANDOM_DRAW.getCode());

        strategy.random = new Random(12345L);
        stubBatchInsert();
        when(matchMapper.insert(any())).thenReturn(1);
        strategy.generate(ctx1);
        ArgumentCaptor<BasketV4Match> captor1 = ArgumentCaptor.forClass(BasketV4Match.class);
        verify(matchMapper, times(4)).insert(captor1.capture());
        List<String> pairs1 = pairKeys(captor1.getAllValues());

        org.mockito.Mockito.reset(matchMapper, scheduleBatchMapper);
        stubBatchInsert();
        when(matchMapper.insert(any())).thenReturn(1);
        strategy.random = new Random(12345L);
        strategy.generate(ctx2);
        ArgumentCaptor<BasketV4Match> captor2 = ArgumentCaptor.forClass(BasketV4Match.class);
        verify(matchMapper, times(4)).insert(captor2.capture());
        List<String> pairs2 = pairKeys(captor2.getAllValues());

        assertEquals(pairs1, pairs2);
    }

    @Test
    void enum_randomDraw_labelAndFromCode() {
        assertEquals("随机抽签", PairingRuleEnum.RANDOM_DRAW.getDescription());
        assertEquals(PairingRuleEnum.RANDOM_DRAW, PairingRuleEnum.fromCode("RANDOM_DRAW"));
    }

    private void assertTeamCoverage(List<BasketV4Match> matches, int expectedTeamCount) {
        Set<Long> teams = new HashSet<>();
        for (BasketV4Match m : matches) {
            assertTrue(teams.add(m.getHomeEventTeamId()), "重复主队: " + m.getHomeEventTeamId());
            assertTrue(teams.add(m.getAwayEventTeamId()), "重复客队: " + m.getAwayEventTeamId());
        }
        assertEquals(expectedTeamCount, teams.size());
    }

    private List<String> pairKeys(List<BasketV4Match> matches) {
        return matches.stream()
                .map(m -> m.getHomeEventTeamId() + "-" + m.getAwayEventTeamId())
                .toList();
    }

    private void stubBatchInsert() {
        when(scheduleBatchMapper.insert(any())).thenAnswer(invocation -> {
            BasketScheduleBatch batch = invocation.getArgument(0);
            batch.setId(999L);
            return 1;
        });
    }

    private void stubSeriesInsertFrom(long startId) {
        when(seriesMapper.selectByStageId(10L)).thenReturn(List.of());
        AtomicInteger seriesId = new AtomicInteger((int) startId);
        when(seriesMapper.insert(any())).thenAnswer(invocation -> {
            BasketMatchSeries series = invocation.getArgument(0);
            series.setId((long) seriesId.getAndIncrement());
            return 1;
        });
    }

    private StageExecutionContext buildSeriesContext(int teamCount,
                                                     int bestOf,
                                                     AdvancementTypeEnum advancementType,
                                                     String nextStageCode) {
        BasketDivisionStage stage = baseStage();
        stage.setBestOf(bestOf);
        stage.setAdvancementType(advancementType.getCode());
        stage.setNextStageCode(nextStageCode);
        return buildContext(stage, teamCount);
    }

    private StageExecutionContext buildBo1Context(int teamCount,
                                                  AdvancementTypeEnum advancementType,
                                                  String nextStageCode) {
        BasketDivisionStage stage = baseStage();
        stage.setBestOf(1);
        stage.setAdvancementType(advancementType.getCode());
        stage.setNextStageCode(nextStageCode);
        return buildContext(stage, teamCount);
    }

    private StageExecutionContext buildContext(BasketDivisionStage stage, int teamCount) {
        BasketDivision division = new BasketDivision();
        division.setId(100L);
        division.setSeasonId(1L);
        division.setPlatformCode("CNPA");
        division.setFormatType("CUSTOM");

        StageExecutionContext context = new StageExecutionContext();
        context.setDivision(division);
        context.setStage(stage);
        context.setStageTeams(teams(teamCount));
        context.setExecutionVersion(1);
        return context;
    }

    private BasketDivisionStage baseStage() {
        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setId(10L);
        stage.setDivisionId(100L);
        stage.setStageCode("QF");
        stage.setStageType(StageTypeEnum.KNOCKOUT.getCode());
        stage.setOrganizationMode(OrganizationModeEnum.UNGROUPED.getCode());
        stage.setCompetitionFormat(CompetitionFormatEnum.SINGLE_ELIMINATION.getCode());
        stage.setGroupingMode(GroupingModeEnum.NONE.getCode());
        stage.setPairingRule(PairingRuleEnum.HIGH_VS_LOW.getCode());
        return stage;
    }

    private static List<BasketDivisionStageTeam> teams(int count) {
        List<BasketDivisionStageTeam> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            BasketDivisionStageTeam team = new BasketDivisionStageTeam();
            team.setEventTeamId((long) i);
            team.setSeedNo(i);
            list.add(team);
        }
        return list;
    }
}
