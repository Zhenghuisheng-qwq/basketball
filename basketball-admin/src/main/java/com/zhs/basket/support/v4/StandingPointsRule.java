package com.zhs.basket.support.v4;

import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.common.exception.ServiceException;

/**
 * Standing points capability. Only ROUND_ROBIN stages (including grouped Cup groups)
 * consume winPoints / lossPoints. Legacy defaults match pre-fix Standing: 2 / 1.
 */
public final class StandingPointsRule {

    public static final int LEGACY_WIN_POINTS = 2;
    public static final int LEGACY_LOSS_POINTS = 1;

    private StandingPointsRule() {
    }

    public static boolean supportsCompetitionFormat(String competitionFormat) {
        return CompetitionFormatEnum.ROUND_ROBIN.getCode().equalsIgnoreCase(competitionFormat);
    }

    public static int winPointsOrLegacy(Integer configured) {
        return configured != null ? configured : LEGACY_WIN_POINTS;
    }

    public static int lossPointsOrLegacy(Integer configured) {
        return configured != null ? configured : LEGACY_LOSS_POINTS;
    }

    public static void requireNonNegative(Integer winPoints, Integer lossPoints, String label) {
        if (winPoints != null && winPoints < 0) {
            throw new ServiceException(label + " winPoints must be >= 0");
        }
        if (lossPoints != null && lossPoints < 0) {
            throw new ServiceException(label + " lossPoints must be >= 0");
        }
    }
}