package com.zhs.basket.support.v4.roster;

import com.zhs.basket.domain.v4.BasketRegistration;
import com.zhs.basket.enums.v4.ParticipationRoleEnum;
import com.zhs.basket.enums.v4.RosterAllocationStrategyTypeEnum;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Component
public class AutoCapacityAllocationStrategy implements RosterAllocationStrategy {

    @Override
    public String type() {
        return RosterAllocationStrategyTypeEnum.AUTO_CAPACITY.getCode();
    }

    @Override
    public RosterAllocationPlan allocate(List<BasketRegistration> allocatable, int formalCap) {
        List<BasketRegistration> sorted = allocatable.stream()
                .sorted(Comparator
                        .comparing(BasketRegistration::getSubmitTime, Comparator.nullsLast(Date::compareTo))
                        .thenComparing(BasketRegistration::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
        RosterAllocationPlan plan = new RosterAllocationPlan();
        int candidateOrder = 1;
        for (int i = 0; i < sorted.size(); i++) {
            BasketRegistration row = sorted.get(i);
            if (i < formalCap) {
                plan.add(row.getId(), ParticipationRoleEnum.FORMAL.getCode(), null);
            } else {
                plan.add(row.getId(), ParticipationRoleEnum.CANDIDATE.getCode(), candidateOrder++);
            }
        }
        return plan;
    }
}
