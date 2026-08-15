package com.zhs.support.v4;

import com.zhs.exception.ServiceException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MatchSubmissionPlayerStatRulesTest {

    @Test
    void emptyOrNull_ok() {
        assertDoesNotThrow(() -> MatchSubmissionPlayerStatRules.assertNoDuplicateEventPlayers(null));
        assertDoesNotThrow(() -> MatchSubmissionPlayerStatRules.assertNoDuplicateEventPlayers(Collections.emptyList()));
    }

    @Test
    void uniquePlayers_ok() {
        assertDoesNotThrow(() -> MatchSubmissionPlayerStatRules.assertNoDuplicateEventPlayers(Arrays.asList(1L, 2L, 3L)));
    }

    @Test
    void duplicatePlayers_rejected() {
        assertThrows(ServiceException.class,
                () -> MatchSubmissionPlayerStatRules.assertNoDuplicateEventPlayers(Arrays.asList(1L, 2L, 1L)));
    }
}
