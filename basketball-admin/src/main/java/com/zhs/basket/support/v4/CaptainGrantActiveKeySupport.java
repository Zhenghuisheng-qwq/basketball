package com.zhs.basket.support.v4;

/**
 * 队长授权 ACTIVE 唯一键：division_id + player_id。
 */
public final class CaptainGrantActiveKeySupport {

    private CaptainGrantActiveKeySupport() {
    }

    public static String buildActiveUniqueKey(Long divisionId, Long playerId) {
        if (divisionId == null || playerId == null) {
            return null;
        }
        return divisionId + "_" + playerId;
    }
}
