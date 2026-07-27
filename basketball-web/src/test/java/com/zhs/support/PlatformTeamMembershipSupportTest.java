package com.zhs.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformTeamMembershipSupportTest {

    @Test
    void normalizePlatformCode_mapsPs5ToPs() {
        assertEquals("PS", GameTerminalSupport.normalize("PS5"));
        assertEquals("PC", GameTerminalSupport.normalize("pc"));
        assertEquals("XBOX", GameTerminalSupport.normalize("Xbox"));
    }

    @Test
    void matches_allowsCrossAliasWithinSamePlatform() {
        assertTrue(GameTerminalSupport.matches("PS5", "PS"));
        assertTrue(GameTerminalSupport.matches("PC", "PC"));
    }

    @Test
    void resolvePlayerTeamMismatchReason_returnsNullWhenPlatformsMatch() {
        assertNull(GameTerminalSupport.resolvePlayerTeamMismatchReason("PS5", "PS"));
    }

    @Test
    void resolvePlayerTeamMismatchReason_returnsReasonWhenPlatformsDiffer() {
        String reason = GameTerminalSupport.resolvePlayerTeamMismatchReason("PC", "PS");
        assertTrue(reason != null && reason.contains("PS"));
    }
}
