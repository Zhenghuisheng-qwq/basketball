package com.zhs.basket.support;

/**
 * 阶段是否生成传统积分榜。
 */
public final class StageStandingSupport {

    private StageStandingSupport() {}

    public static boolean isKnockoutStage(String stageType, String formatType) {
        return containsKnockout(stageType) || containsKnockout(formatType);
    }

    public static boolean supportsTraditionalStanding(String stageType, String formatType) {
        return !isKnockoutStage(stageType, formatType);
    }

    private static boolean containsKnockout(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String v = value.trim().toUpperCase();
        return v.contains("KNOCKOUT")
                || v.contains("PLAYOFF")
                || v.contains("ELIMINATION")
                || value.contains("淘汰");
    }
}
