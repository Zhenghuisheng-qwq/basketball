package com.zhs.basket.support.v4.stage;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StandingRankingRuleTest {

    private static final Long A = 100L;
    private static final Long B = 200L;
    private static final Long C = 300L;

    @Mock private BasketDivisionStageMapper stageMapper;
    @Mock private BasketDivisionTieBreakRuleMapper tieBreakRuleMapper;
    @Mock private BasketV4MatchMapper matchMapper;
    @Mock private BasketStandingMapper standingMapper;
    @Mock private BasketStandingRankReasonMapper rankReasonMapper;
    @Mock private BasketPointAdjustmentMapper pointAdjustmentMapper;
    @Spy private TieBreakComparatorBuilder comparatorBuilder = new TieBreakComparatorBuilder();

    @InjectMocks
    private TieBreakStandingService service;

    @BeforeEach
    void setUp() {
        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setId(20L);
        stage.setDivisionId(10L);
        stage.setCompetitionFormat("ROUND_ROBIN");
        stage.setWinPoints(2);
        stage.setLossPoints(1);
        when(stageMapper.selectById(20L)).thenReturn(stage);
        when(tieBreakRuleMapper.selectByStageId(20L)).thenReturn(officialRules());
        org.mockito.Mockito.lenient().when(standingMapper.selectByStageScope(anyLong(), anyLong(), anyLong())).thenReturn(List.of());
        org.mockito.Mockito.lenient().when(pointAdjustmentMapper.selectV4SumByStage(10L, 20L)).thenReturn(List.of());
    }

    @Test
    void case1_pointsBeatBetterDifferential() {
        List<BasketV4Match> matches = new ArrayList<>();
        matches.addAll(repeat(finished(A, C, 22, 20), 4));
        matches.addAll(repeat(finished(A, C, 20, 22), 2));
        matches.addAll(repeat(finished(B, C, 40, 10), 3));
        matches.addAll(repeat(finished(B, C, 10, 12), 3));
        when(matchMapper.selectFinishedByStageId(20L)).thenReturn(matches);

        service.rebuildStageStanding(20L);

        assertEquals(10, standingOf(A).getStandingPoints());
        assertEquals(9, standingOf(B).getStandingPoints());
        assertTrue(standingOf(B).getPointDiff() > standingOf(A).getPointDiff());
        assertTrue(standingOf(A).getRankNo() < standingOf(B).getRankNo());
    }

    @Test
    void case2_samePoints_betterDiffWins() {
        List<BasketV4Match> matches = new ArrayList<>();
        matches.addAll(repeat(finished(A, C, 22, 10), 4));
        matches.addAll(repeat(finished(A, C, 10, 12), 2));
        matches.addAll(repeat(finished(B, C, 21, 20), 4));
        matches.addAll(repeat(finished(B, C, 10, 12), 2));
        when(matchMapper.selectFinishedByStageId(20L)).thenReturn(matches);

        service.rebuildStageStanding(20L);

        assertEquals(10, standingOf(A).getStandingPoints());
        assertEquals(10, standingOf(B).getStandingPoints());
        assertTrue(standingOf(A).getPointDiff() > standingOf(B).getPointDiff());
        assertTrue(standingOf(A).getRankNo() < standingOf(B).getRankNo());
    }

    @Test
    void case3_samePointsSameDiff_headToHeadWins() {
        List<BasketV4Match> matches = new ArrayList<>();
        matches.add(finished(A, B, 10, 8));
        matches.addAll(repeat(finished(A, C, 20, 10), 3));
        matches.addAll(repeat(finished(A, C, 10, 16), 2));
        matches.addAll(repeat(finished(B, C, 20, 12), 4));
        matches.add(finished(B, C, 10, 20));
        when(matchMapper.selectFinishedByStageId(20L)).thenReturn(matches);

        service.rebuildStageStanding(20L);

        assertEquals(10, standingOf(A).getStandingPoints());
        assertEquals(10, standingOf(B).getStandingPoints());
        assertEquals(standingOf(A).getPointDiff(), standingOf(B).getPointDiff());
        assertTrue(standingOf(A).getRankNo() < standingOf(B).getRankNo());
    }

    @Test
    void case4_headToHeadCannotOverridePoints() {
        List<BasketV4Match> matches = new ArrayList<>();
        matches.add(finished(B, A, 80, 70));
        matches.addAll(repeat(finished(A, C, 22, 20), 4));
        matches.add(finished(A, C, 20, 22));
        matches.addAll(repeat(finished(B, C, 22, 20), 2));
        matches.addAll(repeat(finished(B, C, 20, 22), 3));
        when(matchMapper.selectFinishedByStageId(20L)).thenReturn(matches);

        service.rebuildStageStanding(20L);

        assertEquals(10, standingOf(A).getStandingPoints());
        assertEquals(9, standingOf(B).getStandingPoints());
        assertTrue(standingOf(A).getRankNo() < standingOf(B).getRankNo());
    }

    @Test
    void case5_headToHeadCannotOverrideDifferential() {
        List<BasketV4Match> matches = new ArrayList<>();
        matches.add(finished(B, A, 80, 70));
        matches.addAll(repeat(finished(A, C, 40, 10), 4));
        matches.add(finished(A, C, 10, 12));
        matches.addAll(repeat(finished(B, C, 21, 20), 3));
        matches.addAll(repeat(finished(B, C, 10, 12), 2));
        when(matchMapper.selectFinishedByStageId(20L)).thenReturn(matches);

        service.rebuildStageStanding(20L);

        assertEquals(10, standingOf(A).getStandingPoints());
        assertEquals(10, standingOf(B).getStandingPoints());
        assertTrue(standingOf(A).getPointDiff() > standingOf(B).getPointDiff());
        assertTrue(standingOf(A).getRankNo() < standingOf(B).getRankNo());
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

    private BasketStanding standingOf(Long eventTeamId) {
        ArgumentCaptor<BasketStanding> captor = ArgumentCaptor.forClass(BasketStanding.class);
        verify(standingMapper, org.mockito.Mockito.atLeastOnce()).insert(captor.capture());
        return captor.getAllValues().stream()
                .filter(row -> eventTeamId.equals(row.getEventTeamId()))
                .reduce((left, right) -> right)
                .orElseThrow();
    }
}