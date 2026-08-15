package com.zhs.service.support;

public final class PublicKnockoutRoundNames {

    private PublicKnockoutRoundNames() {
    }

    public static String name(Integer roundNo, Integer maxRound) {
        if (roundNo == null) {
            return "--";
        }
        if (maxRound == null || maxRound < 1 || roundNo < 1) {
            return "R" + roundNo;
        }
        int shift = maxRound - roundNo + 1;
        if (shift < 1 || shift > 8) {
            return "R" + roundNo;
        }
        int remaining = 1 << shift;
        if (remaining <= 2) {
            return "Final";
        }
        if (remaining == 4) {
            return "SF";
        }
        return "R" + remaining;
    }
}
