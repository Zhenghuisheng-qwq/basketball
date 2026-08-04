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

    public static boolean isAllowedTournamentTerminal(String terminal) {
        String normalized = normalize(terminal);
        return "PC".equals(normalized) || "PS".equals(normalized) || "XBOX".equals(normalized);
    }

    public static void requireTournamentTerminal(String terminal) {
        if (!StringUtils.hasText(terminal)) {
            throw new com.zhs.common.exception.ServiceException("请选择游戏端");
        }
        if ("ALL".equalsIgnoreCase(terminal.trim())) {
            throw new com.zhs.common.exception.ServiceException("创建赛事时游戏端不能选择全部");
        }
        if (!isAllowedTournamentTerminal(terminal)) {
            throw new com.zhs.common.exception.ServiceException("游戏端仅支持 PC / PS / Xbox");
        }
    }
}
