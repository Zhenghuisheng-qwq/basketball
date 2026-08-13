package com.zhs.basket.support.v4.bracket;

public final class ParticipantSourceLabels {

    private ParticipantSourceLabels() {
    }

    public static String displayCode(String groupCode, Integer rank) {
        if (groupCode == null || rank == null) {
            return null;
        }
        return groupCode + rank;
    }

    public static String displayName(String groupCode, Integer rank) {
        if (groupCode == null || rank == null) {
            return null;
        }
        return groupCode + "组第" + rank + "名";
    }
}
