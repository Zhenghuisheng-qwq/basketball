package com.zhs.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TeamMembershipSupportSlotTest {

    private final TeamMembershipSupport support = new TeamMembershipSupport();

    @Test
    void legacySlotStillUsesMemberIdOnly() {
        assertEquals("2081010979604316162", support.buildActiveMemberSlot(2081010979604316162L));
    }

    @Test
    void divisionScopedSlotUsesDivisionAndMember() {
        assertEquals("118_2081010979604316162",
                support.buildActiveMemberSlot(118L, 2081010979604316162L));
    }

    @Test
    void divisionScopedSlotRequiresDivision() {
        assertThrows(RuntimeException.class, () -> support.buildActiveMemberSlot(null, 1L));
    }
}
