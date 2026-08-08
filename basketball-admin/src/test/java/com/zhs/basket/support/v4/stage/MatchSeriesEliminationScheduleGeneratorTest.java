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
import com.zhs.basket.enums.v4.MatchSeriesStatusEnum;
import com.zhs.basket.enums.v4.MatchStatusEnum;
import com.zhs.basket.enums.v4.OrganizationModeEnum;
import com.zhs.basket.enums.v4.StageTypeEnum;
import com.zhs.basket.mapper.v4.BasketMatchSeriesMapper;
import com.zhs.basket.mapper.v4.BasketScheduleBatchMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.support.v4.ByeQualificationService;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchSeriesEliminationScheduleGeneratorTest {

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
    void caseA_eightTeamsBo3_generatesFourSeriesTwelveGames() {
        StageExecutionContext context = buildSeriesContext(8, 3, AdvancementTypeEnum.WINNER_ADVANCE, "FINAL");
        stubBatchInsert();
        stubSeriesInsertFrom(1L);
        when(matchMapper.insert(any())).thenReturn(1);

        ScheduleGenerationResult result = strategy.generate(context);

        assertEquals(12, result.getExpectedMatchCount());
        verify(seriesMapper, times(4)).insert(any(BasketMatchSeries.class));
        verify(matchMapper, times(12)).insert(any(BasketV4Match.class));
    }

    @Test
    void caseB_fourTeamsBo5_generatesTwoSeriesTenGames() {
        StageExecutionContext context = buildSeriesContext(4, 5, AdvancementTypeEnum.WINNER_ADVANCE, "FINAL");
        stubBatchInsert();
        stubSeriesInsertFrom(10L);
        when(matchMapper.insert(any())).thenReturn(1);

        ScheduleGenerationResult result = strategy.generate(context);

        assertEquals(10, result.getExpectedMatchCount());
        verify(seriesMapper, times(2)).insert(any(BasketMatchSeries.class));
        verify(matchMapper, times(10)).insert(any(BasketV4Match.class));
    }

    @Test
    void caseC_twoTeamsBo5Final_generatesOneSeriesFiveGames() {
        StageExecutionContext context = buildSeriesContext(2, 5, AdvancementTypeEnum.NONE, null);
        stubBatchInsert();
        stubSeriesInsertFrom(20L);
        when(matchMapper.insert(any())).thenReturn(1);

        ScheduleGenerationResult result = strategy.generate(context);

        assertEquals(5, result.getExpectedMatchCount());
        verify(seriesMapper, times(1)).insert(any(BasketMatchSeries.class));
        verify(matchMapper, times(5)).insert(any(BasketV4Match.class));
    }

    @Test
    void bo1NonFinalEightTeams_generatesFourMatchesNotSeven() {
        StageExecutionContext context = buildBo1Context(8, AdvancementTypeEnum.WINNER_ADVANCE, "SF");
        stubBatchInsert();
        when(matchMapper.insert(any())).thenReturn(1);

        ScheduleGenerationResult result = strategy.generate(context);

        assertEquals(4, result.getExpectedMatchCount());
        verify(seriesMapper, times(0)).insert(any());
        verify(matchMapper, times(4)).insert(any(BasketV4Match.class));
    }

    @Test
    void bo1FinalEightTeams_generatesSevenMatchFullBracket() {
        StageExecutionContext context = buildBo1Context(8, AdvancementTypeEnum.NONE, null);
        stubBatchInsert();
        AtomicInteger matchId = new AtomicInteger(100);
        when(matchMapper.insert(any())).thenAnswer(invocation -> {
            BasketV4Match match = invocation.getArgument(0);
            match.setId((long) matchId.getAndIncrement());
            return 1;
        });

        ScheduleGenerationResult result = strategy.generate(context);

        assertEquals(7, result.getExpectedMatchCount());
        verify(matchMapper, times(7)).insert(any(BasketV4Match.class));
    }

    @Test
    void caseD_duplicateSeries_rejected() {
        StageExecutionContext context = buildSeriesContext(8, 3, AdvancementTypeEnum.WINNER_ADVANCE, "FINAL");
        when(seriesMapper.selectByStageId(10L)).thenReturn(List.of(new BasketMatchSeries()));

        ServiceException ex = assertThrows(ServiceException.class, () -> strategy.generate(context));
        assertTrue(ex.getMessage().contains("已存在系列赛"));
    }

    @Test
    void caseE_sevenTeamsBo3_generatesThreeSeriesNineGamesAndBye() {
        StageExecutionContext context = buildSeriesContext(7, 3, AdvancementTypeEnum.WINNER_ADVANCE, "SF");
        stubBatchInsert();
        stubSeriesInsertFrom(30L);
        when(matchMapper.insert(any())).thenReturn(1);

        ScheduleGenerationResult result = strategy.generate(context);

        assertEquals(9, result.getExpectedMatchCount());
        verify(seriesMapper, times(3)).insert(any(BasketMatchSeries.class));
        verify(matchMapper, times(9)).insert(any(BasketV4Match.class));
    }

    @Test
    void caseE_sevenTeamsBo1NonFinal_generatesThreeMatches() {
        StageExecutionContext context = buildBo1Context(7, AdvancementTypeEnum.WINNER_ADVANCE, "SF");
        stubBatchInsert();
        when(matchMapper.insert(any())).thenReturn(1);

        ScheduleGenerationResult result = strategy.generate(context);

        assertEquals(3, result.getExpectedMatchCount());
        verify(matchMapper, times(3)).insert(any(BasketV4Match.class));
    }

    @Test
    void caseG_bo1Final_doesNotCreateSeries() {
        StageExecutionContext context = buildBo1Context(8, AdvancementTypeEnum.NONE, null);
        stubBatchInsert();
        AtomicInteger matchId = new AtomicInteger(100);
        when(matchMapper.insert(any())).thenAnswer(invocation -> {
            BasketV4Match match = invocation.getArgument(0);
            match.setId((long) matchId.getAndIncrement());
            return 1;
        });

        strategy.generate(context);

        verify(seriesMapper, times(0)).insert(any());
        ArgumentCaptor<BasketV4Match> captor = ArgumentCaptor.forClass(BasketV4Match.class);
        verify(matchMapper, times(7)).insert(captor.capture());
        for (BasketV4Match match : captor.getAllValues()) {
            assertNull(match.getSeriesId());
        }
    }

    @Test
    void preview_bo1NonFinal_showsFourMatches() {
        StageExecutionContext context = buildBo1Context(8, AdvancementTypeEnum.WINNER_ADVANCE, "SF");
        StageSchedulePreviewVo preview = strategy.preview(context);
        assertEquals(4, preview.getTotalMatches());
        assertEquals(1, preview.getRoundCount());
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
        BasketDivision division = new BasketDivision();
        division.setId(100L);
        division.setSeasonId(1L);
        division.setPlatformCode("CNPA");
        division.setFormatType("CUSTOM");
        division.setMatchStartTime(new Date(1_700_000_000_000L));

        BasketDivisionStage stage = baseStage();
        stage.setBestOf(bestOf);
        stage.setAdvancementType(advancementType.getCode());
        stage.setNextStageCode(nextStageCode);

        return buildContext(division, stage, teamCount);
    }

    private StageExecutionContext buildBo1Context(int teamCount,
                                                  AdvancementTypeEnum advancementType,
                                                  String nextStageCode) {
        BasketDivision division = new BasketDivision();
        division.setId(100L);
        division.setSeasonId(1L);
        division.setPlatformCode("CNPA");
        division.setFormatType("CUSTOM");

        BasketDivisionStage stage = baseStage();
        stage.setBestOf(1);
        stage.setAdvancementType(advancementType.getCode());
        stage.setNextStageCode(nextStageCode);
        return buildContext(division, stage, teamCount);
    }

    private StageExecutionContext buildContext(BasketDivision division, BasketDivisionStage stage, int teamCount) {
        List<BasketDivisionStageTeam> teams = new ArrayList<>();
        for (int i = 1; i <= teamCount; i++) {
            BasketDivisionStageTeam team = new BasketDivisionStageTeam();
            team.setEventTeamId((long) i);
            team.setSeedNo(i);
            teams.add(team);
        }
        StageExecutionContext context = new StageExecutionContext();
        context.setDivision(division);
        context.setStage(stage);
        context.setStageTeams(teams);
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
        return stage;
    }
}
