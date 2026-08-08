package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.enums.v4.AdvancementTypeEnum;
import org.springframework.util.StringUtils;

/**
 * 单败淘汰阶段语义：线性 Stage 链 vs 最终完整 bracket。
 */
public final class EliminationStageSupport {

    private EliminationStageSupport() {
    }

    /** 非最终淘汰阶段：仅生成/执行当前一轮（BO1/BO3/BO5 通用）。 */
    public static boolean isSingleRoundNonFinalStage(BasketDivisionStage stage) {
        if (stage == null) {
            return false;
        }
        return AdvancementTypeEnum.WINNER_ADVANCE.getCode().equals(stage.getAdvancementType())
                && StringUtils.hasText(stage.getNextStageCode());
    }

    /** 最终淘汰阶段：BO1 可完整 bracket；BO3/BO5 仅 2 队 1 Series。 */
    public static boolean isFinalEliminationStage(BasketDivisionStage stage) {
        if (stage == null) {
            return false;
        }
        return AdvancementTypeEnum.NONE.getCode().equals(stage.getAdvancementType())
                && !StringUtils.hasText(stage.getNextStageCode());
    }

    public static boolean isWinnerAdvanceNonFinalStage(BasketDivisionStage stage) {
        return isSingleRoundNonFinalStage(stage);
    }
}
