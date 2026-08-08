package com.zhs.basket.support.v4;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GroupDistributionSupportTest {

    @Test
    void fourteenTeamsFourGroups_evenDistribution() {
        List<Integer> sizes = GroupDistributionSupport.evenGroupSizes(14, 4);
        assertEquals(List.of(4, 4, 3, 3), sizes);
    }

    @Test
    void fiveTeamsFourGroups() {
        List<Integer> sizes = GroupDistributionSupport.evenGroupSizes(5, 4);
        assertEquals(List.of(2, 1, 1, 1), sizes);
    }
}
