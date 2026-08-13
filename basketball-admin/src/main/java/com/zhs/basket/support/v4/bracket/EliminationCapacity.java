package com.zhs.basket.support.v4.bracket;

import com.zhs.common.exception.ServiceException;
import lombok.Getter;

/**
 * Single-elimination capacity math: nextPowerOfTwo, BYE, first-round actual, total series.
 */
@Getter
public final class EliminationCapacity {

    private final int participantCount;
    private final int capacity;
    private final int byeCount;
    private final int firstRoundActual;
    private final int totalActualSeries;
    private final int roundCount;

    private EliminationCapacity(int participantCount, int capacity, int byeCount,
                                int firstRoundActual, int totalActualSeries, int roundCount) {
        this.participantCount = participantCount;
        this.capacity = capacity;
        this.byeCount = byeCount;
        this.firstRoundActual = firstRoundActual;
        this.totalActualSeries = totalActualSeries;
        this.roundCount = roundCount;
    }

    public static int nextPowerOfTwo(int n) {
        if (n < 2) {
            throw new ServiceException("淘汰赛参赛单位数至少为 2");
        }
        int value = 1;
        while (value < n) {
            value <<= 1;
        }
        return value;
    }

    public static EliminationCapacity of(int participantCount) {
        int capacity = nextPowerOfTwo(participantCount);
        int byeCount = capacity - participantCount;
        int firstRoundActual = participantCount - capacity / 2;
        int totalActualSeries = participantCount - 1;
        int roundCount = Integer.numberOfTrailingZeros(capacity);
        return new EliminationCapacity(participantCount, capacity, byeCount,
                firstRoundActual, totalActualSeries, roundCount);
    }
}
