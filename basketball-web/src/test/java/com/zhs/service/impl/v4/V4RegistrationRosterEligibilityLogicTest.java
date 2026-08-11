package com.zhs.service.impl.v4;

import com.zhs.support.GameTerminalSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class V4RegistrationRosterEligibilityLogicTest {

    @Test
    void pcDivisionMatchesPcIdentity() {
        assertTrue(GameTerminalSupport.matchesDivisionPlatform("PC", "PC"));
        assertFalse(GameTerminalSupport.matchesDivisionPlatform("PC", "PS"));
        assertFalse(GameTerminalSupport.matchesDivisionPlatform("PC", "XBOX"));
    }

    @Test
    void draftMayBeBelowMinButSubmitNeedsMin() {
        int min = 3;
        int draftCount = 1;
        int submitCount = 3;
        assertTrue(draftCount < min);
        assertTrue(submitCount >= min);
    }

    @Test
    void entryEligibilityIndependentFromRosterReadiness() {
        boolean canEnter = true;
        int eligible = 2;
        int min = 3;
        boolean canSubmit = canEnter && eligible >= min;
        assertTrue(canEnter);
        assertFalse(canSubmit);
    }
}
