package com.zhs.basket.support.v4.leaderboard;

public final class LeaderboardScopes {

    public static final String OVERALL = "OVERALL";
    public static final String QUALIFIED_ONLY = "QUALIFIED_ONLY";
    public static final String ALL_PARTICIPANTS = "ALL_PARTICIPANTS";

    private LeaderboardScopes() {
    }

    public static String stage(Long stageId) {
        if (stageId == null) {
            throw new IllegalArgumentException("stageId 不能为空");
        }
        return "STAGE:" + stageId;
    }

    public static boolean isOverall(String scopeKey) {
        return OVERALL.equals(scopeKey);
    }

    public static Long stageId(String scopeKey) {
        if (scopeKey == null || !scopeKey.startsWith("STAGE:")) {
            return null;
        }
        return Long.valueOf(scopeKey.substring("STAGE:".length()));
    }
}
