package com.zhs.support.v4;

import com.zhs.response.v4.publicapi.V4PublicStandingItemResp;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Public standing win rate: wins / (wins + losses) * 100, 1 decimal.
 * POINT_DEDUCTION does not change wins/losses, so winRate stays the same.
 */
public final class V4PublicStandingWinRateSupport {

    private V4PublicStandingWinRateSupport() {
    }

    public static void fill(V4PublicStandingItemResp row) {
        if (row == null) {
            return;
        }
        row.setWinRate(winRate(row.getWinCount(), row.getLoseCount()));
    }

    public static BigDecimal winRate(Integer winCount, Integer loseCount) {
        int wins = winCount == null ? 0 : winCount;
        int losses = loseCount == null ? 0 : loseCount;
        int played = wins + losses;
        if (played <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(wins)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(played), 1, RoundingMode.HALF_UP);
    }
}