package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;

/**
 * MatchSeries schedule generation is available for SE and RR when bestOf is 3/5/7.
 */
public final class MatchSeriesExecutionCapability {

    private MatchSeriesExecutionCapability() {
    }

    public static boolean supportsScheduleGeneration(BasketDivisionStage stage) {
        if (stage == null || stage.getBestOf() == null) {
            return false;
        }
        int bestOf = stage.getBestOf();
        if (bestOf != 3 && bestOf != 5 && bestOf != 7) {
            if (!hasMappedSeriesBo(stage)) {
                return false;
            }
        }
        String format = stage.getCompetitionFormat();
        return CompetitionFormatEnum.SINGLE_ELIMINATION.getCode().equals(format)
                || CompetitionFormatEnum.ROUND_ROBIN.getCode().equals(format);
    }

    public static boolean isEliminationSeries(BasketDivisionStage stage) {
        if (stage == null) {
            return false;
        }
        if (!CompetitionFormatEnum.SINGLE_ELIMINATION.getCode().equals(stage.getCompetitionFormat())) {
            return false;
        }
        return BestOfValues.isLegal(stage.getBestOf()) && (stage.getBestOf() > 1 || hasMappedSeriesBo(stage));
    }

    private static boolean hasMappedSeriesBo(BasketDivisionStage stage) {
        Integer sf = stage.getSemifinalBestOf();
        Integer fi = stage.getFinalBestOf();
        return (sf != null && sf > 1) || (fi != null && fi > 1);
    }
}
