package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivisionStage;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StandingPointsFromStageRuleTest {

    @Mock private BasketDivisionStageMapper stageMapper;
    @Mock private BasketDivisionTieBreakRuleMapper tieBreakRuleMapper;
    @Mock private BasketV4MatchMapper matchMapper;
    @Mock private BasketStandingMapper standingMapper;
    @Mock private BasketStandingRankReasonMapper rankReasonMapper;
    @Mock private BasketPointAdjustmentMapper pointAdjustmentMapper;
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
        when(stageMapper.selectById(20L)).thenReturn(stage);
        org.mockito.Mockito.lenient().when(tieBreakRuleMapper.selectByStageId(20L)).thenReturn(List.of());
        org.mockito.Mockito.lenient().when(standingMapper.selectByStageScope(anyLong(), anyLong(), anyLong())).thenReturn(List.of());
        org.mockito.Mockito.lenient().when(pointAdjustmentMapper.selectV4SumByStage(10L, 20L)).thenReturn(List.of());
    }

    @Test
    void threeWinsTwoLosses_withThreeOne_isElevenAndNine() {
        stage.setWinPoints(3);
        stage.setLossPoints(1);
        when(matchMapper.selectFinishedByStageId(20L)).thenReturn(record(3, 2));

        service.rebuildStageStanding(20L);

        BasketStanding a = standingOf(100L);
        BasketStanding b = standingOf(200L);
        assertEquals(11, a.getStandingPoints());
        assertEquals(9, b.getStandingPoints());
        assertEquals(3, a.getWinCount());
        assertEquals(2, a.getLoseCount());
        assertEquals(2, b.getWinCount());
        assertEquals(3, b.getLoseCount());
    }

    @Test
    void sameRecord_withTwoZero_isSixAndFour() {
        stage.setWinPoints(2);
        stage.setLossPoints(0);
        when(matchMapper.selectFinishedByStageId(20L)).thenReturn(record(3, 2));

        service.rebuildStageStanding(20L);

        assertEquals(6, standingOf(100L).getStandingPoints());
        assertEquals(4, standingOf(200L).getStandingPoints());
        assertEquals(3, standingOf(100L).getWinCount());
        assertEquals(2, standingOf(100L).getLoseCount());
    }

    @Test
    void nullSnapshotPoints_useLegacyTwoOne() {
        stage.setWinPoints(null);
        stage.setLossPoints(null);
        when(matchMapper.selectFinishedByStageId(20L)).thenReturn(record(3, 2));

        service.rebuildStageStanding(20L);

        assertEquals(8, standingOf(100L).getStandingPoints());
        assertEquals(7, standingOf(200L).getStandingPoints());
    }

    @Test
    void forfeitUsesConfiguredStagePoints() {
        stage.setWinPoints(3);
        stage.setLossPoints(1);
        stage.setForfeitWinPoints(3);
        stage.setForfeitLossPoints(1);
        when(matchMapper.selectFinishedByStageId(20L)).thenReturn(List.of(forfeit(100L, 200L)));

        service.rebuildStageStanding(20L);

        assertEquals(3, standingOf(100L).getStandingPoints());
        assertEquals(1, standingOf(200L).getStandingPoints());
        assertEquals(1, standingOf(100L).getWinCount());
        assertEquals(1, standingOf(200L).getLoseCount());
    }

    @Test
    void historicalForfeitTwoZeroUnchanged() {
        stage.setWinPoints(2);
        stage.setLossPoints(1);
        stage.setForfeitWinPoints(2);
        stage.setForfeitLossPoints(0);
        when(matchMapper.selectFinishedByStageId(20L)).thenReturn(List.of(forfeit(100L, 200L)));

        service.rebuildStageStanding(20L);

        assertEquals(2, standingOf(100L).getStandingPoints());
        assertEquals(0, standingOf(200L).getStandingPoints());
    }

    @Test
    void dqForfeitUsesConfiguredPoints() {
        stage.setWinPoints(3);
        stage.setLossPoints(1);
        stage.setForfeitWinPoints(3);
        stage.setForfeitLossPoints(1);
        BasketV4Match dq = forfeit(200L, 100L);
        when(matchMapper.selectFinishedByStageId(20L)).thenReturn(List.of(dq));

        service.rebuildStageStanding(20L);

        assertEquals(1, standingOf(100L).getStandingPoints());
        assertEquals(3, standingOf(200L).getStandingPoints());
    }

    private List<BasketV4Match> record(int aWins, int bWins) {
        List<BasketV4Match> matches = new ArrayList<>();
        for (int i = 0; i < aWins; i++) {
            matches.add(finished(100L, 200L, 80, 70));
        }
        for (int i = 0; i < bWins; i++) {
            matches.add(finished(100L, 200L, 70, 80));
        }
        return matches;
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

    private BasketV4Match forfeit(Long winnerId, Long loserId) {
        BasketV4Match match = new BasketV4Match();
        match.setSeasonId(1L);
        match.setMatchStatus(MatchStatusEnum.FORFEITED.getCode());
        match.setWinnerEventTeamId(winnerId);
        match.setLoserEventTeamId(loserId);
        return match;
    }

    private BasketStanding standingOf(Long eventTeamId) {
        ArgumentCaptor<BasketStanding> captor = ArgumentCaptor.forClass(BasketStanding.class);
        verify(standingMapper, org.mockito.Mockito.atLeastOnce()).insert(captor.capture());
        return captor.getAllValues().stream()
                .filter(row -> eventTeamId.equals(row.getEventTeamId()))
                .reduce((a, b) -> b)
                .orElseThrow();
    }
}
