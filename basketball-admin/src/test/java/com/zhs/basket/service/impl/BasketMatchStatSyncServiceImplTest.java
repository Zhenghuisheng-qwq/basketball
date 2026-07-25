package com.zhs.basket.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BasketMatchStatSyncServiceImplTest {

    @Test
    void buildKeyIsStable() {
        String key = BasketMatchStatSyncServiceImpl.buildKey(100L, 200L, "MATCH_CONFIRM");
        assertEquals("MATCH:100:OR:200:TYPE:MATCH_CONFIRM", key);
    }
}
