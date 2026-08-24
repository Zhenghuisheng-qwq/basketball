package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.BasketPointAdjustment;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketDivisionTieBreakRule;
import com.zhs.basket.domain.v4.BasketStanding;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.enums.v4.MatchStatusEnum;
import com.zhs.basket.mapper.BasketPointAdjustmentMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageMapper;
import com.zhs.basket.mapper.v4.BasketDivisionTieBreakRuleMapper;
import com.zhs.basket.mapper.v4.BasketStandingMapper;
import com.zhs.basket.mapper.v4.BasketStandingRankReasonMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.support.standing.DisciplineStandingAdjustmentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TieBreakStandingServicePointDeductionTest {

    private static final Long A = 100L;
    private static final Long B = 200L;
    private static final Long C = 300L;

    @Mock private BasketDivisionStageMapper stageMapper;
    @Mock private BasketDivisionTieBreakRuleMapper tieBreakRuleMapper;
    @Mock private BasketV4MatchMapper matchMapper;
    @Mock private BasketStandingMapper standingMapper;
    @Mock private BasketStandingRankReasonMapper rankReasonMapper;
    @Mock private BasketPointAdjustmentMapper pointAdjustmentMapper;
    @Mock private DisciplineStandingAdjustmentResolver disciplineStandingAdjustmentResolver;
    @Spy private TieBreakComparatorBuilder comparatorBuilder = new TieBreakComparatorBuilder();

    @InjectMocks
    private TieBreakStandingService service;

    private BasketDivisionStage stage;

    @BeforeEach
    void setUp() {
        stage = new BasketDivisionStage();
        stage.setId(20L);
        stage.setDivisionId(10L);
        stage.setCompetitionFormat("ROUND_ROBIN");
        stage.setWinPoints(2);
        stage.setLossPoints(1);
        stage.setForfeitWinPoints(2);
        stage.setForfeitLossPoints(0);
        when(stageMapper.selectById(20L)).thenReturn(stage);
        org.mockito.Mockito.lenient().when(tieBreakRuleMapper.selectByStageId(20L)).thenReturn(officialRules());
        org.mockito.Mockito.lenient().when(standingMapper.selectByStageScope(anyLong(), anyLong(), anyLong()))
                .thenReturn(List.of());
        org.mockito.Mockito.lenient().when(pointAdjustmentMapper.selectV4SumByStage(10L, 20L)).thenReturn(List.of());
        org.mockito.Mockito.lenient().when(disciplineStandingAdjustmentResolver.resolveByEventTeamIds(any()))
                .thenReturn(Map.of());
    }

    @Test
    void case1_threeWinsOneLossDeduct2LeavesRecordIntact() {
        when(matchMapper.selectFinishedByStageId(20L)).thenReturn(List.of(
                finished(A, B, 80, 70),
                finished(A, B, 80, 70),
                finished(A, B, 80, 70),
                finished(B, A, 80, 70)));
        when(disciplineStandingAdjustmentResolver.resolveByEventTeamIds(any())).thenReturn(Map.of(A, -2));

        service.rebuildStageStanding(20L);

        BasketStanding row = standingOf(A);
        assertEquals(5, row.getStandingPoints());
        assertEquals(3, row.getWinCount());
        assertEquals(1, row.getLoseCount());
        assertEquals(310, row.getPointFor());
        assertEquals(290, row.getPointAgainst());
        assertEquals(20, row.getPointDiff());
    }

    @Test
    void case2_finalPointsBeatBetterDifferential() {
        List<BasketV4Match> matches = new ArrayList<>();
        matches.addAll(repeat(finished(A, C, 100, 50), 5));
        matches.addAll(repeat(finished(B, C, 60, 50), 4));
        when(matchMapper.selectFinishedByStageId(20L)).thenReturn(matches);
        when(disciplineStandingAdjustmentResolver.resolveByEventTeamIds(any())).thenReturn(Map.of(A, -3));

        service.rebuildStageStanding(20L);

        assertEquals(7, standingOf(A).getStandingPoints());
        assertEquals(8, standingOf(B).getStandingPoints());
        assertTrue(standingOf(A).getPointDiff() > standingOf(B).getPointDiff());
        assertTrue(standingOf(B).getRankNo() < standingOf(A).getRankNo());
    }

    @Test
    void case3_sameFinalPointsBetterDiffWins() {
        List<BasketV4Match> matches = new ArrayList<>();
        matches.addAll(repeat(finished(A, C, 40, 10), 4));
        matches.addAll(repeat(finished(B, C, 30, 10), 4));
        when(matchMapper.selectFinishedByStageId(20L)).thenReturn(matches);

        service.rebuildStageStanding(20L);

        assertEquals(8, standingOf(A).getStandingPoints());
        assertEquals(8, standingOf(B).getStandingPoints());
        assertTrue(standingOf(A).getPointDiff() > standingOf(B).getPointDiff());
        assertTrue(standingOf(A).getRankNo() < standingOf(B).getRankNo());
    }

    @Test
    void case4_samePointsSameDiffHeadToHeadWins() {
        List<BasketV4Match> matches = new ArrayList<>();
        matches.add(finished(A, B, 10, 8));
        matches.addAll(repeat(finished(A, C, 20, 10), 3));
        matches.addAll(repeat(finished(A, C, 10, 16), 2));
        matches.addAll(repeat(finished(B, C, 20, 12), 4));
        matches.add(finished(B, C, 10, 20));
        when(matchMapper.selectFinishedByStageId(20L)).thenReturn(matches);
        when(disciplineStandingAdjustmentResolver.resolveByEventTeamIds(any())).thenReturn(Map.of(A, -2, B, -2));

        service.rebuildStageStanding(20L);

        assertEquals(8, standingOf(A).getStandingPoints());
        assertEquals(8, standingOf(B).getStandingPoints());
        assertEquals(standingOf(A).getPointDiff(), standingOf(B).getPointDiff());
        assertTrue(standingOf(A).getRankNo() < standingOf(B).getRankNo());
    }

    @Test
    void case5_multipleDeductionsAccumulate() {
        when(matchMapper.selectFinishedByStageId(20L)).thenReturn(repeat(finished(A, B, 80, 70), 5));
        when(disciplineStandingAdjustmentResolver.resolveByEventTeamIds(any())).thenReturn(Map.of(A, -5));

        service.rebuildStageStanding(20L);

        assertEquals(5, standingOf(A).getStandingPoints());
        assertEquals(5, standingOf(A).getWinCount());
    }

    @Test
    void case6_revokeRestoresBasePoints() {
        when(matchMapper.selectFinishedByStageId(20L)).thenReturn(repeat(finished(A, B, 80, 70), 5));
        when(disciplineStandingAdjustmentResolver.resolveByEventTeamIds(any())).thenReturn(Map.of(A, -3));
        service.rebuildStageStanding(20L);
        assertEquals(7, lastStandingOf(A).getStandingPoints());

        when(disciplineStandingAdjustmentResolver.resolveByEventTeamIds(any())).thenReturn(Map.of());
        service.rebuildStageStanding(20L);

        assertEquals(10, lastStandingOf(A).getStandingPoints());
    }

    @Test
    void case7_rebuildFiveTimesIsIdempotent() {
        when(matchMapper.selectFinishedByStageId(20L)).thenReturn(repeat(finished(A, B, 80, 70), 5));
        when(disciplineStandingAdjustmentResolver.resolveByEventTeamIds(any())).thenReturn(Map.of(A, -3));

        for (int i = 0; i < 5; i++) {
            service.rebuildStageStanding(20L);
        }

        assertEquals(7, lastStandingOf(A).getStandingPoints());
        verify(disciplineStandingAdjustmentResolver, times(5)).resolveByEventTeamIds(any());
    }

    @Test
    void negativeFinalPointsAreAllowed() {
        when(matchMapper.selectFinishedByStageId(20L)).thenReturn(List.of(finished(A, B, 80, 70)));
        when(disciplineStandingAdjustmentResolver.resolveByEventTeamIds(any())).thenReturn(Map.of(A, -5));

        service.rebuildStageStanding(20L);

        assertEquals(-3, standingOf(A).getStandingPoints());
        assertEquals(1, standingOf(A).getWinCount());
    }

    @Test
    void case9_forfeitPointsPlusDeduction() {
        BasketV4Match forfeit = new BasketV4Match();
        forfeit.setSeasonId(1L);
        forfeit.setMatchStatus(MatchStatusEnum.FORFEITED.getCode());
        forfeit.setWinnerEventTeamId(A);
        forfeit.setLoserEventTeamId(B);
        when(matchMapper.selectFinishedByStageId(20L)).thenReturn(List.of(forfeit));
        when(disciplineStandingAdjustmentResolver.resolveByEventTeamIds(any())).thenReturn(Map.of(A, -2));

        service.rebuildStageStanding(20L);

        assertEquals(0, standingOf(A).getStandingPoints());
        assertEquals(1, standingOf(A).getWinCount());
        assertEquals(0, standingOf(A).getPointDiff());
        assertEquals(0, standingOf(B).getStandingPoints());
    }

    @Test
    void case10_dqForfeitPlusDeductionStacks() {
        BasketV4Match dq = new BasketV4Match();
        dq.setSeasonId(1L);
        dq.setMatchStatus(MatchStatusEnum.FORFEITED.getCode());
        dq.setHomeEventTeamId(A);
        dq.setAwayEventTeamId(B);
        dq.setWinnerEventTeamId(B);
        dq.setLoserEventTeamId(A);
        when(matchMapper.selectFinishedByStageId(20L)).thenReturn(List.of(dq));
        when(disciplineStandingAdjustmentResolver.resolveByEventTeamIds(any())).thenReturn(Map.of(A, -2));

        service.rebuildStageStanding(20L);

        assertEquals(-2, standingOf(A).getStandingPoints());
        assertEquals(1, standingOf(A).getLoseCount());
        assertEquals(2, standingOf(B).getStandingPoints());
        assertEquals(0, standingOf(A).getPointDiff());
    }

    @Test
    void manualAdjustmentAndDisciplineBothApply() {
        when(matchMapper.selectFinishedByStageId(20L)).thenReturn(repeat(finished(A, B, 80, 70), 6));
        when(pointAdjustmentMapper.selectV4SumByStage(10L, 20L)).thenReturn(List.of(sum(A, -1)));
        when(disciplineStandingAdjustmentResolver.resolveByEventTeamIds(any())).thenReturn(Map.of(A, -2));

        service.rebuildStageStanding(20L);

        assertEquals(9, standingOf(A).getStandingPoints());
    }

    @Test
    void cupGroupStandingAppliesDeductionInsideGroup() {
        stage.setOrganizationMode("GROUPED");
        BasketV4Match match = finished(A, B, 80, 70);
        match.setGroupId(8L);
        when(matchMapper.selectFinishedByStageId(20L)).thenReturn(List.of(match));
        when(disciplineStandingAdjustmentResolver.resolveByEventTeamIds(any())).thenReturn(Map.of(A, -3));

        service.rebuildStageStanding(20L);

        assertEquals(-1, standingOf(A).getStandingPoints());
        assertEquals(1, standingOf(B).getStandingPoints());
        assertEquals(8L, standingOf(A).getGroupScopeKey());
    }

    @Test
    void resolverIsCalledOncePerScopeNotPerTeam() {
        when(matchMapper.selectFinishedByStageId(20L)).thenReturn(List.of(finished(A, B, 80, 70)));

        service.rebuildStageStanding(20L);

        verify(disciplineStandingAdjustmentResolver, times(1)).resolveByEventTeamIds(Set.of(A, B));
    }

    private List<BasketDivisionTieBreakRule> officialRules() {
        return List.of(rule("POINT_DIFFERENCE", 1), rule("HEAD_TO_HEAD", 2));
    }

    private BasketDivisionTieBreakRule rule(String code, int priority) {
        BasketDivisionTieBreakRule row = new BasketDivisionTieBreakRule();
        row.setRuleCode(code);
        row.setPriorityNo(priority);
        row.setEnabled(1);
        return row;
    }

    private List<BasketV4Match> repeat(BasketV4Match match, int times) {
        List<BasketV4Match> list = new ArrayList<>();
        for (int i = 0; i < times; i++) {
            list.add(match);
        }
        return list;
    }

    private BasketV4Match finished(Long home, Long away, int homeScore, int awayScore) {
        BasketV4Match match = new BasketV4Match();
        match.setSeasonId(1L);
        match.setMatchStatus(MatchStatusEnum.FINISHED.getCode());
        match.setHomeEventTeamId(home);
        match.setAwayEventTeamId(away);
        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);
        if (homeScore > awayScore) {
            match.setWinnerEventTeamId(home);
            match.setLoserEventTeamId(away);
        } else {
            match.setWinnerEventTeamId(away);
            match.setLoserEventTeamId(home);
        }
        return match;
    }

    private BasketPointAdjustment sum(Long eventTeamId, int points) {
        BasketPointAdjustment row = new BasketPointAdjustment();
        row.setEventTeamId(eventTeamId);
        row.setAdjustmentPoints(points);
        return row;
    }

    private BasketStanding standingOf(Long eventTeamId) {
        return lastStandingOf(eventTeamId);
    }

    private BasketStanding lastStandingOf(Long eventTeamId) {
        ArgumentCaptor<BasketStanding> captor = ArgumentCaptor.forClass(BasketStanding.class);
        verify(standingMapper, org.mockito.Mockito.atLeastOnce()).insert(captor.capture());
        return captor.getAllValues().stream()
                .filter(row -> eventTeamId.equals(row.getEventTeamId()))
                .reduce((left, right) -> right)
                .orElseThrow();
    }
}
