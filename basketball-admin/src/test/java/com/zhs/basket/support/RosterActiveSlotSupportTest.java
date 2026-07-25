package com.zhs.basket.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RosterActiveSlotSupportTest {

    @Test
    void buildActiveSlot_usesSeasonAndPlayerId() {
        assertEquals("100:200", RosterActiveSlotSupport.buildActiveSlot(100L, 200L));
    }

    @Test
    void buildActiveSlot_isUniquePerSeasonPlayerPair() {
        String slot1 = RosterActiveSlotSupport.buildActiveSlot(1L, 10L);
        String slot2 = RosterActiveSlotSupport.buildActiveSlot(1L, 11L);
        String slot3 = RosterActiveSlotSupport.buildActiveSlot(2L, 10L);
        assertNotEquals(slot1, slot2);
        assertNotEquals(slot1, slot3);
        assertNotEquals(slot2, slot3);
    }

    @Test
    void buildActiveSlot_samePairProducesSameValue() {
        assertEquals(
                RosterActiveSlotSupport.buildActiveSlot(99L, 88L),
                RosterActiveSlotSupport.buildActiveSlot(99L, 88L));
    }
}
