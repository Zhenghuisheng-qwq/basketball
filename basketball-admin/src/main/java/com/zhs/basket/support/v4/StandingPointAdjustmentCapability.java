package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;

public final class StandingPointAdjustmentCapability {

    private StandingPointAdjustmentCapability() {
    }

    public static boolean canAdjustStandingPoints(BasketDivisionStage stage) {
        return stage != null
                && CompetitionFormatEnum.ROUND_ROBIN.getCode().equals(stage.getCompetitionFormat());
    }
}
