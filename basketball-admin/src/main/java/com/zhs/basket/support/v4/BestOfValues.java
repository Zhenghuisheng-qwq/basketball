package com.zhs.basket.support.v4;

import com.zhs.common.exception.ServiceException;

/**
 * BO legal values: 1 / 3 / 5 / 7.
 */
public final class BestOfValues {

    private BestOfValues() {
    }

    public static boolean isLegal(Integer bestOf) {
        return bestOf != null && (bestOf == 1 || bestOf == 3 || bestOf == 5 || bestOf == 7);
    }

    public static void requireLegal(Integer bestOf) {
        if (!isLegal(bestOf)) {
            throw new ServiceException("best_of 仅支持 1、3、5、7");
        }
    }

    public static void requireLegal(Integer bestOf, String label) {
        if (!isLegal(bestOf)) {
            throw new ServiceException(label + " only allows 1/3/5/7");
        }
    }

    public static int requiredWins(int bestOf) {
        requireLegal(bestOf);
        return bestOf / 2 + 1;
    }
}
