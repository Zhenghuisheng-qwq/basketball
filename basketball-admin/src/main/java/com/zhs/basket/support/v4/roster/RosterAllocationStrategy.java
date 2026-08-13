package com.zhs.basket.support.v4.roster;

import com.zhs.basket.domain.v4.BasketRegistration;

import java.util.List;

public interface RosterAllocationStrategy {
    String type();

    RosterAllocationPlan allocate(List<BasketRegistration> allocatable, int formalCap);
}
