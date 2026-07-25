package com.zhs.basket.support;

public final class RosterActiveSlotSupport {

    private RosterActiveSlotSupport() {
    }

    public static String buildActiveSlot(Long seasonId, Long playerId) {
        return seasonId + ":" + playerId;
    }
}
