package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.BasketPointAdjustment;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TieBreakStandingServicePointAdjustmentTest {

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
        stage.setWinPoints(2);
        stage.setLossPoints(1);
        stage.setForfeitWinPoints(2);
        stage.setForfeitLossPoints(0);
        when(stageMapper.selectById(20L)).thenReturn(stage);
        org.mockito.Mockito.lenient().when(tieBreakRuleMapper.selectByStageId(20L)).thenReturn(List.of());
        org.mockito.Mockito.lenient().when(standingMapper.selectByStageScope(anyLong(), anyLong(), anyLong())).thenReturn(List.of());
    }

    @Test
    void caseA_base12Minus2Equals10() {
        when(matchMapper.selectFinishedByStageId(20L)).thenReturn(wins(6));
        when(pointAdjustmentMapper.selectV4SumByStage(10L, 20L)).thenReturn(List.of(sum(100L, -2)));

        service.rebuildStageStanding(20L);

        assertEquals(10, pointsOf(100L));
    }

    @Test
    void caseB_secondDeltaKeepsSum() {
        when(matchMapper.selectFinishedByStageId(20L)).thenReturn(wins(6));
        when(pointAdjustmentMapper.selectV4SumByStage(10L, 20L)).thenReturn(List.of(sum(100L, -1)));

        service.rebuildStageStanding(20L);

        assertEquals(11, pointsOf(100L));
    }

    @Test
    void caseC_rebuildDoesNotStackAdjustment() {
        when(matchMapper.selectFinishedByStageId(20L)).thenReturn(wins(6));
        when(pointAdjustmentMapper.selectV4SumByStage(10L, 20L)).thenReturn(List.of(sum(100L, -1)));

        service.rebuildStageStanding(20L);
        service.rebuildStageStanding(20L);

        assertEquals(11, lastPointsOf(100L));
        verify(pointAdjustmentMapper, times(2)).selectV4SumByStage(10L, 20L);
    }

    @Test
    void caseD_resultCorrectionKeepsAdjustment() {
        when(matchMapper.selectFinishedByStageId(20L)).thenReturn(wins(7));
        when(pointAdjustmentMapper.selectV4SumByStage(10L, 20L)).thenReturn(List.of(sum(100L, -1)));

        service.rebuildStageStanding(20L);

        assertEquals(13, pointsOf(100L));
    }

    @Test
    void caseE_forfeitBasePlusAdjustment() {
        BasketV4Match forfeit = new BasketV4Match();
        forfeit.setSeasonId(1L);
        forfeit.setMatchStatus(MatchStatusEnum.FORFEITED.getCode());
        forfeit.setWinnerEventTeamId(100L);
        forfeit.setLoserEventTeamId(200L);
        when(matchMapper.selectFinishedByStageId(20L)).thenReturn(List.of(forfeit));
        when(pointAdjustmentMapper.selectV4SumByStage(10L, 20L)).thenReturn(List.of(sum(100L, -1)));

        service.rebuildStageStanding(20L);

        assertEquals(1, pointsOf(100L));
        assertEquals(0, pointsOf(200L));
    }

    @Test
    void caseF_otherDivisionAdjustmentNotApplied() {
        when(matchMapper.selectFinishedByStageId(20L)).thenReturn(wins(6));
        when(pointAdjustmentMapper.selectV4SumByStage(10L, 20L)).thenReturn(List.of());

        service.rebuildStageStanding(20L);

        assertEquals(12, pointsOf(100L));
    }

    @Test
    void revokeLastForfeit_clearsStanding() {
        when(matchMapper.selectFinishedByStageId(20L)).thenReturn(List.of());
        BasketStanding leftover = new BasketStanding();
        leftover.setEventTeamId(100L);
        leftover.setGroupScopeKey(0L);
        leftover.setStandingPoints(2);
        when(standingMapper.selectByStageId(20L)).thenReturn(List.of(leftover));

        service.rebuildStageStanding(20L);

        verify(standingMapper).deleteByStageScope(10L, 20L, 0L);
    }

    @Test
    void cupGroupStandingAppliesAdjustment() {
        stage.setOrganizationMode("GROUPED");
        BasketV4Match match = finished(100L, 200L, 80, 70);
        match.setGroupId(8L);
        when(matchMapper.selectFinishedByStageId(20L)).thenReturn(List.of(match));
        when(pointAdjustmentMapper.selectV4SumByStage(10L, 20L)).thenReturn(List.of(sum(100L, -3)));

        service.rebuildStageStanding(20L);

        assertEquals(-1, pointsOf(100L));
        assertEquals(1, pointsOf(200L));
    }

    private int pointsOf(Long eventTeamId) {
        return lastPointsOf(eventTeamId);
    }

    private int lastPointsOf(Long eventTeamId) {
        ArgumentCaptor<BasketStanding> captor = ArgumentCaptor.forClass(BasketStanding.class);
        verify(standingMapper, times(standingInsertCount())).insert(captor.capture());
        return captor.getAllValues().stream()
                .filter(row -> eventTeamId.equals(row.getEventTeamId()))
                .reduce((a, b) -> b)
                .map(BasketStanding::getStandingPoints)
                .orElseThrow();
    }

    private int standingInsertCount() {
        return (int) org.mockito.Mockito.mockingDetails(standingMapper).getInvocations().stream()
                .filter(inv -> "insert".equals(inv.getMethod().getName()))
                .count();
    }

    private List<BasketV4Match> wins(int count) {
        List<BasketV4Match> matches = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            matches.add(finished(100L, 200L, 80, 70));
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
        return match;
    }

    private BasketPointAdjustment sum(Long eventTeamId, int points) {
        BasketPointAdjustment row = new BasketPointAdjustment();
        row.setEventTeamId(eventTeamId);
        row.setAdjustmentPoints(points);
        return row;
    }
}
