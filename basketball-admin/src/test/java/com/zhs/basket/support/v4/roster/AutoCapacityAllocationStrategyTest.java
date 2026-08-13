package com.zhs.basket.support.v4.roster;

import com.zhs.basket.domain.v4.BasketRegistration;
import com.zhs.basket.enums.v4.ParticipationRoleEnum;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoCapacityAllocationStrategyTest {

    private final AutoCapacityAllocationStrategy strategy = new AutoCapacityAllocationStrategy();

    @Test
    void sixApproved_cap4_fourFormalTwoCandidate() {
        List<BasketRegistration> rows = new ArrayList<>();
        long base = 1000L;
        for (int i = 0; i < 6; i++) {
            BasketRegistration row = new BasketRegistration();
            row.setId(base + i);
            row.setSubmitTime(new Date(base + i));
            rows.add(row);
        }
        RosterAllocationPlan plan = strategy.allocate(rows, 4);
        assertEquals(6, plan.getItems().size());
        assertEquals(ParticipationRoleEnum.FORMAL.getCode(), plan.getItems().get(0).getParticipationRole());
        assertEquals(ParticipationRoleEnum.FORMAL.getCode(), plan.getItems().get(3).getParticipationRole());
        assertEquals(ParticipationRoleEnum.CANDIDATE.getCode(), plan.getItems().get(4).getParticipationRole());
        assertEquals(1, plan.getItems().get(4).getCandidateOrder());
        assertEquals(2, plan.getItems().get(5).getCandidateOrder());
    }
}
