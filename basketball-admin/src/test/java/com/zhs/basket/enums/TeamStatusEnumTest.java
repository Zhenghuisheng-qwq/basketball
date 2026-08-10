package com.zhs.basket.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TeamStatusEnumTest {

    @Test
    void archivedCodeIsThree() {
        assertEquals(3, TeamStatusEnum.ARCHIVED.getCode());
        assertEquals("已归档", TeamStatusEnum.ARCHIVED.getDescription());
        assertEquals(TeamStatusEnum.ARCHIVED, TeamStatusEnum.fromCode(3));
    }

    @Test
    void legacyCodesUnchanged() {
        assertEquals(0, TeamStatusEnum.DISABLED.getCode());
        assertEquals(1, TeamStatusEnum.ACTIVE.getCode());
        assertEquals(2, TeamStatusEnum.DISSOLVED.getCode());
    }

    @Test
    void fromCodeRejectsUnknown() {
        assertThrows(RuntimeException.class, () -> TeamStatusEnum.fromCode(99));
    }
}
