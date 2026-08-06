package com.zhs.basket.support;

import com.zhs.common.utils.StringUtils;

public final class GameTerminalSupport {

    private GameTerminalSupport() {
    }

    public static String normalize(String terminal) {
        if (!StringUtils.hasText(terminal)) {
            return null;
        }
        String normalized = terminal.trim().toUpperCase();
        if ("PS5".equals(normalized) || "PSN".equals(normalized)) {
            return "PS";
        }
        return normalized;
    }

    public static String formatDisplayName(String terminal) {
        String normalized = normalize(terminal);
        if (normalized == null) {
            return "未设置";
        }
        return switch (normalized) {
            case "PC" -> "PC";
            case "PS_XBOX" -> "PS/Xbox";
            case "PS" -> "PS（历史）";
            case "XBOX" -> "Xbox（历史）";
            default -> normalized;
        };
    }

    public static boolean matchesDivisionPlatform(String divisionPlatform, String accountPlatform) {
        String div = normalize(divisionPlatform);
        String acc = normalize(accountPlatform);
        if (div == null || acc == null) {
            return false;
        }
        if ("PS_XBOX".equals(div)) {
            return "PS".equals(acc) || "XBOX".equals(acc);
        }
        return div.equals(acc);
    }
}
