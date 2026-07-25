package com.zhs.basket.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MatchDataStatusEnumTest {

    @Test
    void rejectedCanEditButNotConfirm() {
        MatchDataStatusEnum rejected = MatchDataStatusEnum.REJECTED;
        assertTrue(rejected.canEdit());
        assertFalse(rejected.canConfirm());
    }

    @Test
    void submittedCanConfirm() {
        MatchDataStatusEnum submitted = MatchDataStatusEnum.SUBMITTED;
        assertTrue(submitted.canConfirm());
    }

    @Test
    void fromCodeSupportsRejected() {
        assertEquals(MatchDataStatusEnum.REJECTED, MatchDataStatusEnum.fromCode(4));
    }
}
