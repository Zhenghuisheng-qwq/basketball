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
            case "PS" -> "PS";
            case "XBOX" -> "Xbox";
            default -> normalized;
        };
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
