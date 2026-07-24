package com.zhs.basket.support;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 统计计算工具：命中率 / 场均统一精度。
 * 百分比字段使用 4 位小数（与 DDL decimal(7,4) 对齐），场均使用 2 位小数。
 */
public final class StatMathUtils {

    public static final int PCT_SCALE = 4;
    public static final int AVG_SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private StatMathUtils() {}

    public static BigDecimal percentage(int made, int attempted) {
        if (attempted <= 0) {
            return BigDecimal.ZERO.setScale(PCT_SCALE, ROUNDING);
        }
        return BigDecimal.valueOf(made)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(attempted), PCT_SCALE, ROUNDING);
    }

    public static BigDecimal percentage(long made, long attempted) {
        if (attempted <= 0) {
            return BigDecimal.ZERO.setScale(PCT_SCALE, ROUNDING);
        }
        return BigDecimal.valueOf(made)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(attempted), PCT_SCALE, ROUNDING);
    }

    public static BigDecimal average(long total, int games) {
        if (games <= 0) {
            return BigDecimal.ZERO.setScale(AVG_SCALE, ROUNDING);
        }
        return BigDecimal.valueOf(total).divide(BigDecimal.valueOf(games), AVG_SCALE, ROUNDING);
    }

    public static BigDecimal average(BigDecimal total, int games) {
        if (games <= 0 || total == null) {
            return BigDecimal.ZERO.setScale(AVG_SCALE, ROUNDING);
        }
        return total.divide(BigDecimal.valueOf(games), AVG_SCALE, ROUNDING);
    }

    public static BigDecimal winRate(int wins, int games) {
        if (games <= 0) {
            return BigDecimal.ZERO.setScale(PCT_SCALE, ROUNDING);
        }
        return BigDecimal.valueOf(wins)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(games), PCT_SCALE, ROUNDING);
    }

    public static BigDecimal minutesAverage(int totalSeconds, int games) {
        if (games <= 0) {
            return BigDecimal.ZERO.setScale(AVG_SCALE, ROUNDING);
        }
        return BigDecimal.valueOf(totalSeconds)
                .divide(BigDecimal.valueOf(60), 6, ROUNDING)
                .divide(BigDecimal.valueOf(games), AVG_SCALE, ROUNDING);
    }

    public static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    public static long nzLong(Integer v) {
        return v == null ? 0L : v.longValue();
    }
}
