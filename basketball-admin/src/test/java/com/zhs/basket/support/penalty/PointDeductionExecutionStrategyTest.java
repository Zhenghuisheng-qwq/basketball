package com.zhs.basket.support.penalty;

import com.zhs.basket.domain.BasketDisciplineRecord;
import com.zhs.basket.enums.PenaltyExecutionStatusEnum;
import com.zhs.basket.support.standing.PointDeductionStandingSyncService;
import com.zhs.basket.vo.PenaltyExecutionResultVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PointDeductionExecutionStrategyTest {

    @Mock private PointDeductionStandingSyncService standingSyncService;
    @InjectMocks private PointDeductionExecutionStrategy strategy;

    @Test
    void supportsOnlyEventTeamPointDeduction() {
        assertTrue(strategy.supports(deduction(231L)));
        BasketDisciplineRecord dq = deduction(231L);
        dq.setPenaltyCode("EVENT_DISQUALIFICATION");
        assertFalse(strategy.supports(dq));
        BasketDisciplineRecord player = deduction(231L);
        player.setTargetType("PLAYER");
        assertFalse(strategy.supports(player));
    }

    @Test
    void executeRestoreAndRetryAllRebuildStanding() {
        BasketDisciplineRecord record = deduction(231L);

        PenaltyExecutionResultVo result = strategy.execute(record);
        strategy.restoreEligibility(record);
        strategy.syncAfterEffective(record);

        assertEquals(PenaltyExecutionStatusEnum.EFFECTIVE.name(), result.getExecutionStatus());
        assertEquals(231L, result.getEventTeamId());
        verify(standingSyncService, times(3)).rebuildForRecord(record);
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
