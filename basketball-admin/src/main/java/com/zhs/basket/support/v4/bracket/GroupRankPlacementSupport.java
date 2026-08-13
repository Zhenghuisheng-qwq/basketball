package com.zhs.basket.support.v4.bracket;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.enums.v4.PairingRuleEnum;
import com.zhs.basket.support.v4.StageFormatSpecification;

public final class GroupRankPlacementSupport {

    private GroupRankPlacementSupport() {
    }

    public static boolean usesGroupRankPlacement(BasketDivisionStage knockoutStage, BasketDivisionStage previousStage) {
        if (knockoutStage == null || previousStage == null) {
            return false;
        }
        if (!StageFormatSpecification.isGrouped(previousStage)) {
            return false;
        }
        String pairing = knockoutStage.getPairingRule();
        return PairingRuleEnum.GROUP_CROSS.getCode().equals(pairing)
                || PairingRuleEnum.MANUAL.getCode().equals(pairing);
    }
}
