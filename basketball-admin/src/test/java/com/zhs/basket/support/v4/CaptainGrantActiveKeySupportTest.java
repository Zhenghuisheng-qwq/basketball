package com.zhs.basket.support.v4;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CaptainGrantActiveKeySupportTest {

    @Test
    void buildActiveUniqueKey_usesDivisionAndPlayer() {
        assertEquals("12_34", CaptainGrantActiveKeySupport.buildActiveUniqueKey(12L, 34L));
    }

    @Test
    void buildActiveUniqueKey_nullWhenMissingParts() {
        assertNull(CaptainGrantActiveKeySupport.buildActiveUniqueKey(null, 1L));
        assertNull(CaptainGrantActiveKeySupport.buildActiveUniqueKey(1L, null));
    }
}
