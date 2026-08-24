package com.zhs.basket.support.standing;

import com.zhs.basket.domain.BasketDisciplineRecord;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketEventTeam;
import com.zhs.basket.mapper.v4.BasketEventTeamMapper;
import com.zhs.basket.service.v4.IBasketV4StatsSettlementService;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointDeductionStandingSyncServiceTest {

    @Mock private BasketEventTeamMapper eventTeamMapper;
    @Mock private StandingStageResolver standingStageResolver;
    @Mock private IBasketV4StatsSettlementService statsSettlementService;
    @InjectMocks private PointDeductionStandingSyncService service;

    @Test
    void rebuildsUniqueStandingStage() {
        BasketDisciplineRecord record = deduction(231L);
        BasketEventTeam team = new BasketEventTeam();
        team.setId(231L);
        team.setDivisionId(177L);
        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setId(20L);
        when(eventTeamMapper.selectById(231L)).thenReturn(team);
        when(standingStageResolver.resolveStandingStage(177L)).thenReturn(stage);

        service.rebuildForRecord(record);

        verify(statsSettlementService).rebuildStageStats(20L);
    }

    @Test
    void arenaWithoutStandingStageIsNoOp() {
        BasketDisciplineRecord record = deduction(231L);
        BasketEventTeam team = new BasketEventTeam();
        team.setId(231L);
        team.setDivisionId(180L);
        when(eventTeamMapper.selectById(231L)).thenReturn(team);
        when(standingStageResolver.resolveStandingStage(180L)).thenReturn(null);

        service.rebuildForRecord(record);

        verify(statsSettlementService, never()).rebuildStageStats(anyLong());
    }

    @Test
    void missingEventTeamFails() {
        when(eventTeamMapper.selectById(231L)).thenReturn(null);

        assertThrows(ServiceException.class, () -> service.rebuildForRecord(deduction(231L)));
        verify(statsSettlementService, never()).rebuildStageStats(anyLong());
    }

    @Test
    void otherPenaltyIsIgnored() {
        BasketDisciplineRecord record = deduction(231L);
        record.setPenaltyCode("EVENT_DISQUALIFICATION");

        service.rebuildForRecord(record);

        verify(eventTeamMapper, never()).selectById(anyLong());
        verify(statsSettlementService, never()).rebuildStageStats(anyLong());
    }

    private static BasketDisciplineRecord deduction(Long eventTeamId) {
        BasketDisciplineRecord record = new BasketDisciplineRecord();
        record.setPenaltyCode("POINT_DEDUCTION");
        record.setTargetType("EVENT_TEAM");
        record.setEventTeamId(eventTeamId);
        record.setPointDeduction(2);
        return record;
    }
}
