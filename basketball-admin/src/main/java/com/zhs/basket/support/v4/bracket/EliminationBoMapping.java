package com.zhs.basket.support.v4.bracket;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.support.v4.BestOfValues;

/**
 * Maps bracket round index (1-based, capacity-based) to Early / Semifinal / Final BO.
 * BYE does not change round role.
 */
public final class EliminationBoMapping {

    private EliminationBoMapping() {
    }

    public static EliminationBoRoundRole roleOf(int roundIndex, int roundCount) {
        if (roundCount <= 0 || roundIndex <= 0 || roundIndex > roundCount) {
            return EliminationBoRoundRole.EARLY;
        }
        if (roundIndex == roundCount) {
            return EliminationBoRoundRole.FINAL;
        }
        if (roundIndex == roundCount - 1) {
            return EliminationBoRoundRole.SEMIFINAL;
        }
        return EliminationBoRoundRole.EARLY;
    }

    public static int bestOf(BasketDivisionStage stage, int roundIndex, int roundCount) {
        EliminationBoRoundRole role = roleOf(roundIndex, roundCount);
        Integer mapped = switch (role) {
            case FINAL -> firstNonNull(stage.getFinalBestOf(), stage.getBestOf());
            case SEMIFINAL -> firstNonNull(stage.getSemifinalBestOf(), stage.getBestOf());
            case EARLY -> stage.getBestOf();
        };
        int value = mapped == null ? 1 : mapped;
        BestOfValues.requireLegal(value);
        return value;
    }

    public static boolean anySeriesBestOf(BasketDivisionStage stage, int roundCount) {
        for (int round = 1; round <= roundCount; round++) {
            if (bestOf(stage, round, roundCount) > 1) {
                return true;
            }
        }
        return false;
    }

    private static Integer firstNonNull(Integer preferred, Integer fallback) {
        return preferred != null ? preferred : fallback;
    }
}
