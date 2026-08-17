package com.zhs.support.v4;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PublicStatAverageSupport {

    private PublicStatAverageSupport() {
    }

    public static BigDecimal average(Number total, Integer gamesPlayed) {
        if (gamesPlayed == null || gamesPlayed <= 0 || total == null) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(total.doubleValue())
                .divide(BigDecimal.valueOf(gamesPlayed.longValue()), 1, RoundingMode.HALF_UP);
    }
}
