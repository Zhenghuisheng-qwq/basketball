package com.zhs.support;

import com.zhs.model.BasketTournament;
import com.zhs.model.BasketTournamentSeason;
import org.springframework.util.StringUtils;

/**
 * 赛事报名阵容规则：从赛事 formatCode 与赛季 min/max 解析人数要求。
 */
public final class RegistrationRosterRuleSupport {

    private RegistrationRosterRuleSupport() {
    }

    public static final class Rule {
        private final String formatCode;
        private final int teamPlayCount;
        private final int minRosterCount;
        private final int maxRosterCount;
        private final int requiredStartingCount;

        public Rule(String formatCode, int teamPlayCount, int minRosterCount, int maxRosterCount) {
            this.formatCode = formatCode;
            this.teamPlayCount = teamPlayCount;
            this.minRosterCount = minRosterCount;
            this.maxRosterCount = maxRosterCount;
            this.requiredStartingCount = teamPlayCount;
        }

        public String getFormatCode() {
            return formatCode;
        }

        public int getTeamPlayCount() {
            return teamPlayCount;
        }

        public int getMinRosterCount() {
            return minRosterCount;
        }

        public int getMaxRosterCount() {
            return maxRosterCount;
        }

        public int getRequiredStartingCount() {
            return requiredStartingCount;
        }

        public String getFormatDisplay() {
            return teamPlayCount + "v" + teamPlayCount;
        }
    }

    public static Rule resolve(BasketTournamentSeason season, BasketTournament tournament) {
        String formatCode = normalizeFormat(tournament != null ? tournament.getFormatCode() : null);
        int teamPlayCount = playCountFromFormat(formatCode);
        int defaultMin = teamPlayCount;
        int defaultMax = defaultMaxForFormat(formatCode);

        int min = season != null && season.getMinRosterCount() != null && season.getMinRosterCount() > 0
                ? season.getMinRosterCount() : defaultMin;
        int max = season != null && season.getMaxRosterCount() != null && season.getMaxRosterCount() > 0
                ? season.getMaxRosterCount() : defaultMax;
        if (min > max) {
            int tmp = min;
            min = max;
            max = tmp;
        }
        return new Rule(formatCode, teamPlayCount, min, max);
    }

    private static String normalizeFormat(String code) {
        if (!StringUtils.hasText(code)) {
            return "5V5";
        }
        return code.trim().toUpperCase();
    }

    private static int playCountFromFormat(String format) {
        if (format.startsWith("3")) {
            return 3;
        }
        if (format.startsWith("4")) {
            return 4;
        }
        return 5;
    }

    private static int defaultMaxForFormat(String format) {
        if (format.startsWith("3")) {
            return 5;
        }
        if (format.startsWith("4")) {
            return 6;
        }
        return 8;
    }
}
