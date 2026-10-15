package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.enums.v4.GameModeEnum;

public final class CurrentRosterEligibility {

    private CurrentRosterEligibility() {
    }

    public static int minimum(BasketDivision division) {
        if (division == null) {
            return 0;
        }
        if (division.getLineupMinCount() != null && division.getLineupMinCount() > 0) {
            return division.getLineupMinCount();
        }
        GameModeEnum mode = GameModeEnum.fromCode(division.getGameMode());
        return mode == null ? 0 : mode.getMinRosterCount();
    }

    public static String status(String registrationStatus, int currentEligible, int minimum) {
        if ("APPROVED".equals(registrationStatus)) {
            return minimum <= 0 || currentEligible >= minimum ? "ELIGIBLE" : "INELIGIBLE";
        }
        if ("PENDING_REVIEW".equals(registrationStatus)) {
            return "NEEDS_REVIEW";
        }
        return null;
    }
}
