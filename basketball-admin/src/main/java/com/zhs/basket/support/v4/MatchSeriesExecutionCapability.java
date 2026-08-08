package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;

/**
 * BO3/BO5 MatchSeries 执行能力判断（R2-2 起支持单败淘汰赛程生成）。
 */
public final class MatchSeriesExecutionCapability {

    private MatchSeriesExecutionCapability() {
    }

    public static boolean supportsScheduleGeneration(BasketDivisionStage stage) {
        if (stage == null || stage.getBestOf() == null) {
            return false;
        }
        int bestOf = stage.getBestOf();
        if (bestOf != 3 && bestOf != 5) {
            return false;
        }
        return CompetitionFormatEnum.SINGLE_ELIMINATION.getCode().equals(stage.getCompetitionFormat());
    }
}
