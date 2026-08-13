package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.common.exception.ServiceException;

import java.util.List;

/**
 * BO capability gate: 1/3/5/7 are legal. RR and SE series execution is supported.
 */
public final class BestOfCapabilityGate {

    private BestOfCapabilityGate() {
    }

    public static void assertCanOpenRegistration(List<BasketDivisionStage> stages) {
        if (stages == null) {
            return;
        }
        for (BasketDivisionStage stage : stages) {
            assertStageSupportedForOpenRegistration(stage);
        }
    }

    public static void assertCanOpenRegistrationBestOfValues(List<Integer> bestOfValues) {
        if (bestOfValues == null) {
            return;
        }
        for (Integer bestOf : bestOfValues) {
            if (bestOf != null) {
                BestOfValues.requireLegal(bestOf);
            }
        }
    }

    private static void assertStageSupportedForOpenRegistration(BasketDivisionStage stage) {
        if (stage == null) {
            return;
        }
        if (stage.getBestOf() != null) {
            BestOfValues.requireLegal(stage.getBestOf());
        }
        if (stage.getSemifinalBestOf() != null) {
            BestOfValues.requireLegal(stage.getSemifinalBestOf());
        }
        if (stage.getFinalBestOf() != null) {
            BestOfValues.requireLegal(stage.getFinalBestOf());
        }
    }

    public static void assertCanGenerateSchedule(BasketDivisionStage stage) {
        if (stage == null) {
            return;
        }
        if (stage.getBestOf() != null) {
            BestOfValues.requireLegal(stage.getBestOf());
        }
        if (stage.getSemifinalBestOf() != null) {
            BestOfValues.requireLegal(stage.getSemifinalBestOf());
        }
        if (stage.getFinalBestOf() != null) {
            BestOfValues.requireLegal(stage.getFinalBestOf());
        }
    }

    public static boolean hasUnsupportedBestOf(BasketDivisionStage stage) {
        if (stage == null) {
            return false;
        }
        try {
            assertCanGenerateSchedule(stage);
            return false;
        } catch (ServiceException ex) {
            return true;
        }
    }
}
