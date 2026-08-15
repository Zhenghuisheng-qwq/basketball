package com.zhs.service.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicScheduleUnitResolverTest {

    @Test
    void bo1_isMatchProductUnit() {
        assertEquals(PublicScheduleUnitResolver.MATCH, PublicScheduleUnitResolver.unitType(1));
        assertEquals(PublicScheduleUnitResolver.MATCH, PublicScheduleUnitResolver.unitType(null));
        assertFalse(PublicScheduleUnitResolver.exposeAsSeries(1));
    }

    @Test
    void bo3Bo5Bo7_areMatchSeriesProductUnit() {
        assertEquals(PublicScheduleUnitResolver.MATCH_SERIES, PublicScheduleUnitResolver.unitType(3));
        assertEquals(PublicScheduleUnitResolver.MATCH_SERIES, PublicScheduleUnitResolver.unitType(5));
        assertEquals(PublicScheduleUnitResolver.MATCH_SERIES, PublicScheduleUnitResolver.unitType(7));
        assertTrue(PublicScheduleUnitResolver.exposeAsSeries(3));
    }
}
