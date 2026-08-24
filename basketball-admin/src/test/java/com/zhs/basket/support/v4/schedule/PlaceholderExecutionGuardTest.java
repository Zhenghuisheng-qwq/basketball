package com.zhs.basket.support.v4.schedule;

import com.zhs.basket.vo.v4.ScheduleUnitView;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceholderExecutionGuardTest {

    @Test
    void unresolved_blocksExecution() {
        assertFalse(PairingResolutionGuard.isResolved(null, null));
        assertFalse(PairingResolutionGuard.isResolved(0L, 8L));
        ServiceException ex = assertThrows(ServiceException.class,
                () -> PairingResolutionGuard.assertResolvedForExecution(null, 8L));
        assertEquals(PairingResolutionGuard.MSG_EXECUTION_BLOCKED, ex.getMessage());
    }

    @Test
    void resolved_allowsExecution() {
        assertTrue(PairingResolutionGuard.isResolved(1L, 8L));
        assertDoesNotThrow(() -> PairingResolutionGuard.assertResolvedForExecution(1L, 8L));
    }

    @Test
    void illegalPlaceholder_hasNoSource() {
        assertFalse(PairingResolutionGuard.isLegalPlaceholder(null, null, null, null, null, null));
        assertTrue(PairingResolutionGuard.isLegalPlaceholder(null, null, "WINNER_OF_SERIES", "WINNER_OF_SERIES", 1L, 2L));
    }

    @Test
    void legalPlaceholder_isSchedulableWithoutResolvedPairing() {
        ScheduleUnitView unit = new ScheduleUnitView();
        unit.setRequired(true);
        unit.setHomeParticipantId(null);
        unit.setAwayParticipantId(null);
        unit.setHomeSourceType("WINNER_OF_SERIES");
        unit.setAwaySourceType("WINNER_OF_SERIES");
        assertTrue(PairingResolutionGuard.isSchedulable(unit));
        assertDoesNotThrow(() -> PairingResolutionGuard.assertSchedulable(unit));
    }

    @Test
    void illegalPlaceholder_isNotSchedulable() {
        ScheduleUnitView unit = new ScheduleUnitView();
        unit.setRequired(true);
        unit.setHomeParticipantId(null);
        unit.setAwayParticipantId(null);
        assertFalse(PairingResolutionGuard.isSchedulable(unit));
        ServiceException ex = assertThrows(ServiceException.class,
                () -> PairingResolutionGuard.assertSchedulable(unit));
        assertEquals(PairingResolutionGuard.MSG_ILLEGAL_PLACEHOLDER, ex.getMessage());
    }
}
