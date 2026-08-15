package com.zhs.service.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PublicKnockoutRoundNamesTest {

    @Test
    void arena16_roundNames() {
        assertEquals("R16", PublicKnockoutRoundNames.name(1, 4));
        assertEquals("R8", PublicKnockoutRoundNames.name(2, 4));
        assertEquals("SF", PublicKnockoutRoundNames.name(3, 4));
        assertEquals("Final", PublicKnockoutRoundNames.name(4, 4));
    }
}
