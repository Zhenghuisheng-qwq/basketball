package com.zhs.support.v4;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PublicStatAverageSupportTest {

    @Test
    void teamStatAverages_twoGames() {
        int matchCount = 2;
        assertEquals(new BigDecimal("22.0"), PublicStatAverageSupport.average(44, matchCount));
        assertEquals(new BigDecimal("10.0"), PublicStatAverageSupport.average(20, matchCount));
        assertEquals(new BigDecimal("5.0"), PublicStatAverageSupport.average(10, matchCount));
    }

    @Test
    void average_zeroGames_isZero() {
        assertEquals(new BigDecimal("0.0"), PublicStatAverageSupport.average(44, 0));
        assertEquals(new BigDecimal("0.0"), PublicStatAverageSupport.average(44, null));
    }

    @Test
    void playerAverage_total44_games2() {
        assertEquals(new BigDecimal("22.0"), PublicStatAverageSupport.average(44, 2));
    }

    @Test
    void ppgLeader_prefersHigherAverageNotTotal() {
        java.math.BigDecimal playerA = PublicStatAverageSupport.average(44, 4);
        java.math.BigDecimal playerB = PublicStatAverageSupport.average(30, 2);
        assertEquals(new java.math.BigDecimal("11.0"), playerA);
        assertEquals(new java.math.BigDecimal("15.0"), playerB);
        org.junit.jupiter.api.Assertions.assertTrue(playerB.compareTo(playerA) > 0);
    }
}