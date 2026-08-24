package com.zhs.support.v4;

import com.zhs.response.v4.publicapi.V4PublicStandingItemResp;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class V4PublicStandingWinRateSupportTest {

    @Test
    void case8_publicDtoKeepsFinalPointsAndComputesWinRate() {
        V4PublicStandingItemResp vo = new V4PublicStandingItemResp();
        vo.setRankNo(2);
        vo.setEventTeamId(100L);
        vo.setWinCount(3);
        vo.setLoseCount(1);
        vo.setStandingPoints(5);
        vo.setPointFor(100);
        vo.setPointAgainst(80);
        vo.setPointDiff(20);

        V4PublicStandingWinRateSupport.fill(vo);

        assertEquals(2, vo.getRankNo());
        assertEquals(3, vo.getWinCount());
        assertEquals(1, vo.getLoseCount());
        assertEquals(5, vo.getStandingPoints());
        assertEquals(20, vo.getPointDiff());
        assertEquals(new BigDecimal("75.0"), vo.getWinRate());
    }

    @Test
    void zeroGamesIsZeroWinRate() {
        assertEquals(new BigDecimal("0.0"), V4PublicStandingWinRateSupport.winRate(0, 0));
    }

    @Test
    void perfectRecordIsOneHundred() {
        assertEquals(new BigDecimal("100.0"), V4PublicStandingWinRateSupport.winRate(4, 0));
    }

    @Test
    void deductionDoesNotChangeWinRateFormula() {
        assertEquals(new BigDecimal("75.0"), V4PublicStandingWinRateSupport.winRate(3, 1));
    }

    @Test
    void nullRowIsIgnored() {
        V4PublicStandingWinRateSupport.fill(null);
        V4PublicStandingItemResp vo = new V4PublicStandingItemResp();
        assertNull(vo.getWinRate());
    }
}
