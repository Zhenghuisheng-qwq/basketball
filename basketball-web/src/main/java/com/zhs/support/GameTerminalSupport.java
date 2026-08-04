package com.zhs.support;

import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import org.springframework.util.StringUtils;

/**
 * 比赛终端一致性校验与展示辅助。
 */
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

    /** CNPA 普通球队不绑定具体平台，跳过平台/终端一致性校验 */
    public static boolean isGeneralTeam(String terminal) {
        return "GENERAL".equals(normalize(terminal));
    }

    public static boolean isGeneralTeam(BasketTeam team) {
        return team != null && isGeneralTeam(team.getGameTerminal());
    }

    public static boolean matches(String left, String right) {
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);
        if (normalizedLeft == null || normalizedRight == null) {
            return false;
        }
        return normalizedLeft.equals(normalizedRight);
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
            case "MOBILE" -> "手机端";
            case "XBOX" -> "Xbox（历史）";
            default -> normalized;
        };
    }

    /** Division 平台与球员平台账号是否匹配（PS_XBOX 兼容 PS / XBOX） */
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

    public static String resolvePlayerTeamMismatchReason(BasketPlayerProfile player, BasketTeam team) {
        if (player == null || team == null) {
            return "比赛终端不一致，无法加入该球队";
        }
        return resolvePlayerTeamMismatchReason(player.getGameTerminal(), team.getGameTerminal());
    }

    public static String resolvePlayerTeamMismatchReason(String playerTerminal, String teamTerminal) {
        if (!StringUtils.hasText(playerTerminal)) {
            return "请先完善球员比赛终端";
        }
        if (!StringUtils.hasText(teamTerminal)) {
            return "该球队比赛终端未设置，无法加入";
        }
        if (matches(playerTerminal, teamTerminal)) {
            return null;
        }
        return buildDetailedMismatchMessage(playerTerminal, teamTerminal);
    }

    public static String buildApplyJoinMismatchMessage(String playerTerminal, String teamTerminal) {
        if (!StringUtils.hasText(playerTerminal)) {
            return "请先完善球员比赛终端";
        }
        if (!StringUtils.hasText(teamTerminal)) {
            return "该球队比赛终端未设置，无法申请加入";
        }
        if (matches(playerTerminal, teamTerminal)) {
            return null;
        }
        return "当前球员比赛终端为「" + formatDisplayName(playerTerminal)
                + "」，不能申请加入「" + formatDisplayName(teamTerminal) + "」球队";
    }

    public static String buildInviteMismatchMessage(String playerTerminal, String teamTerminal) {
        if (!StringUtils.hasText(playerTerminal)) {
            return "该球员比赛终端未设置，无法邀请";
        }
        if (!StringUtils.hasText(teamTerminal)) {
            return "当前球队比赛终端未设置，无法邀请球员";
        }
        if (matches(playerTerminal, teamTerminal)) {
            return null;
        }
        return "该球员比赛终端为「" + formatDisplayName(playerTerminal)
                + "」，当前球队比赛终端为「" + formatDisplayName(teamTerminal) + "」，无法邀请";
    }

    public static String buildAcceptMismatchMessage(String playerTerminal, String teamTerminal) {
        String reason = resolvePlayerTeamMismatchReason(playerTerminal, teamTerminal);
        if (reason == null) {
            return null;
        }
        if (matches(playerTerminal, teamTerminal)) {
            return null;
        }
        if (!StringUtils.hasText(playerTerminal) || !StringUtils.hasText(teamTerminal)) {
            return "比赛终端不一致，无法加入该球队";
        }
        return "你的比赛终端为「" + formatDisplayName(playerTerminal)
                + "」，该球队比赛终端为「" + formatDisplayName(teamTerminal) + "」，无法跨平台组队";
    }

    public static String buildApproveApplicationMismatchMessage(String playerTerminal, String teamTerminal) {
        if (matches(playerTerminal, teamTerminal)) {
            return null;
        }
        return "申请人比赛终端与球队比赛终端不一致，无法加入";
    }

    public static String buildDetailedMismatchMessage(String playerTerminal, String teamTerminal) {
        if (!StringUtils.hasText(playerTerminal) || !StringUtils.hasText(teamTerminal)) {
            return "比赛终端不一致，无法加入该球队";
        }
        return "你的比赛终端为「" + formatDisplayName(playerTerminal)
                + "」，该球队比赛终端为「" + formatDisplayName(teamTerminal) + "」，无法跨平台组队";
    }
}
