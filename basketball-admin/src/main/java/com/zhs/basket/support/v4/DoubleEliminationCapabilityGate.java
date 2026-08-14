package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.common.exception.ServiceException;

import java.util.List;

/**
 * Double Elimination is reserved/demo only. Formal lock/generate/publish must reject.
 */
public final class DoubleEliminationCapabilityGate {

    public static final String RESERVED_MESSAGE = "当前版本暂未开放双败淘汰正式执行。";

    private DoubleEliminationCapabilityGate() {
    }

    public static boolean isDoubleElimination(String competitionFormat) {
        return CompetitionFormatEnum.DOUBLE_ELIMINATION.getCode().equals(competitionFormat);
    }

    public static boolean isDoubleElimination(BasketDivisionStage stage) {
        return stage != null && isDoubleElimination(stage.getCompetitionFormat());
    }

    public static void assertNotReservedFormat(String competitionFormat) {
        if (isDoubleElimination(competitionFormat)) {
            throw new ServiceException(RESERVED_MESSAGE);
        }
    }

    public static void assertCanGenerateSchedule(BasketDivisionStage stage) {
        if (isDoubleElimination(stage)) {
            throw new ServiceException(RESERVED_MESSAGE);
        }
    }

    public static void assertCanLockOrPublish(List<BasketDivisionStage> stages) {
        if (stages == null) {
            return;
        }
        for (BasketDivisionStage stage : stages) {
            if (isDoubleElimination(stage)) {
                throw new ServiceException(RESERVED_MESSAGE);
            }
        }
    }
}
