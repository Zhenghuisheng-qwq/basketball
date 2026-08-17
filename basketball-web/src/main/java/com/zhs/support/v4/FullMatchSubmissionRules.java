package com.zhs.support.v4;

import com.zhs.exception.ServiceException;
import com.zhs.request.v4.V4MatchSubmissionPeriodScoreReq;
import com.zhs.request.v4.V4MatchSubmissionPlayerStatReq;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Full Match 球员/节比分/汇总校验。错误信息定位到 Game · Team · Player · Field。
 */
public final class FullMatchSubmissionRules {

    public static final String MODE_LEGACY = "LEGACY_TEAM_SIDE";
    public static final String MODE_WINNER_FULL_MATCH = "WINNER_FULL_MATCH";
    /** 首轮曾写入 FULL_MATCH，读取时与 WINNER_FULL_MATCH 等价。 */
    public static final String MODE_FULL_MATCH = "FULL_MATCH";

    public static boolean isWinnerFullMatch(String mode) {
        return MODE_WINNER_FULL_MATCH.equals(mode) || MODE_FULL_MATCH.equals(mode);
    }

    private FullMatchSubmissionRules() {
    }

    public static int nvl(Integer v) {
        return v == null ? 0 : v;
    }

    /**
     * 正式口径：field_goal_* = 总运动战命中/出手（FGM/FGA），3PM 是 FGM 的子集。
     * 2PM = FGM - 3PM
     * PTS = (FGM - 3PM)×2 + 3PM×3 + FTM
     */
    public static int expectedPoints(int fieldGoalMade, int threePointMade, int freeThrowMade) {
        int twoPointMade = fieldGoalMade - threePointMade;
        return twoPointMade * 2 + threePointMade * 3 + freeThrowMade;
    }

    /** 仅 5V5 需要节比分。3V3 不展示、不校验、不落库。 */
    public static boolean requiresPeriodScores(String gameMode) {
        if (gameMode == null || gameMode.isBlank()) {
            return true;
        }
        String code = gameMode.trim().toUpperCase();
        return !("THREE_V_THREE".equals(code) || "3V3".equals(code));
    }

    public static int actualGameCount(int bestOf, int homeWins, int awayWins) {
        return homeWins + awayWins;
    }

    public static void assertValidSeriesScore(int bestOf, int homeWins, int awayWins) {
        if (bestOf < 1) {
            throw new ServiceException("无效的系列赛赛制");
        }
        int need = bestOf / 2 + 1;
        if (homeWins < 0 || awayWins < 0) {
            throw new ServiceException("系列赛比分不能为负数");
        }
        if (homeWins != need && awayWins != need) {
            throw new ServiceException("系列赛最终比分必须有一方先胜 " + need + " 场");
        }
        if (homeWins == need && awayWins == need) {
            throw new ServiceException("系列赛双方不能同时达到获胜场次");
        }
        if (homeWins > need || awayWins > need) {
            throw new ServiceException("系列赛胜场不能超过 " + need);
        }
        if (homeWins + awayWins > bestOf) {
            throw new ServiceException("系列赛实际场次不能超过 BO" + bestOf);
        }
    }

    public static Long declaredWinner(Long homeEventTeamId, Long awayEventTeamId, int homeWins, int awayWins) {
        if (homeWins > awayWins) {
            return homeEventTeamId;
        }
        if (awayWins > homeWins) {
            return awayEventTeamId;
        }
        return null;
    }

    public static void assertNonNegativeInt(String label, Integer value) {
        if (value == null) {
            return;
        }
        if (value < 0) {
            throw new ServiceException(label + "必须为大于等于 0 的整数");
        }
    }

    public static void validatePlayerRow(String prefix, V4MatchSubmissionPlayerStatReq row, boolean requireComplete) {
        if (row == null) {
            return;
        }
        boolean hasValue = hasAnyStat(row);
        if (hasValue && row.getEventPlayerId() == null) {
            throw new ServiceException(prefix + "尚未选择球员");
        }
        if (!hasValue && row.getEventPlayerId() == null) {
            return;
        }
        int pts = nvl(row.getPoints());
        int reb = nvl(row.getRebound());
        int ast = nvl(row.getAssist());
        int stl = nvl(row.getSteal());
        int blk = nvl(row.getBlock());
        int pf = nvl(row.getFoul());
        int tov = nvl(row.getTurnover());
        int fgm = nvl(row.getFieldGoalMade());
        int fga = nvl(row.getFieldGoalAttempt());
        int tpm = nvl(row.getThreePointMade());
        int tpa = nvl(row.getThreePointAttempt());
        int ftm = nvl(row.getFreeThrowMade());
        int fta = nvl(row.getFreeThrowAttempt());
        assertNonNegative(prefix, "PTS", pts);
        assertNonNegative(prefix, "REB", reb);
        assertNonNegative(prefix, "AST", ast);
        assertNonNegative(prefix, "STL", stl);
        assertNonNegative(prefix, "BLK", blk);
        assertNonNegative(prefix, "PF", pf);
        assertNonNegative(prefix, "TOV", tov);
        assertNonNegative(prefix, "FGM", fgm);
        assertNonNegative(prefix, "FGA", fga);
        assertNonNegative(prefix, "3PM", tpm);
        assertNonNegative(prefix, "3PA", tpa);
        assertNonNegative(prefix, "FTM", ftm);
        assertNonNegative(prefix, "FTA", fta);
        if (!requireComplete && !hasValue) {
            return;
        }
        if (fgm > fga) {
            throw new ServiceException(prefix + "：FGM " + fgm + " 大于 FGA " + fga);
        }
        if (tpm > tpa) {
            throw new ServiceException(prefix + "：3PM " + tpm + " 大于 3PA " + tpa);
        }
        if (ftm > fta) {
            throw new ServiceException(prefix + "：FTM " + ftm + " 大于 FTA " + fta);
        }
        if (tpm > fgm) {
            throw new ServiceException(prefix + "：3PM " + tpm + " 大于 FGM " + fgm);
        }
        if (tpa > fga) {
            throw new ServiceException(prefix + "：3PA " + tpa + " 大于 FGA " + fga);
        }
        int expected = expectedPoints(fgm, tpm, ftm);
        if (pts != expected) {
            throw new ServiceException(prefix + "：PTS " + pts + " 与公式 (FGM-3PM)×2+3PM×3+FTM=" + expected + " 不一致");
        }
    }

    public static void validateTeamPlayers(String gameLabel, String teamName, Long eventTeamId,
                                           List<V4MatchSubmissionPlayerStatReq> rows, boolean requireComplete) {
        List<V4MatchSubmissionPlayerStatReq> used = usedRows(rows);
        Set<Long> seen = new HashSet<>();
        int index = 0;
        for (V4MatchSubmissionPlayerStatReq row : used) {
            index++;
            String prefix = formatPrefix(gameLabel, teamName, row, index);
            if (row.getEventPlayerId() != null && !seen.add(row.getEventPlayerId())) {
                throw new ServiceException(prefix + "：同一球队同一场不能重复选择球员");
            }
            if (eventTeamId != null && row.getEventTeamId() != null && !eventTeamId.equals(row.getEventTeamId())) {
                throw new ServiceException(prefix + "：禁止跨队选择球员");
            }
            validatePlayerRow(prefix, row, requireComplete);
        }
        if (requireComplete && used.isEmpty()) {
            throw new ServiceException(formatPrefix(gameLabel, teamName, null, 0) + "至少需要一名球员数据");
        }
    }

    public static int sumPoints(List<V4MatchSubmissionPlayerStatReq> rows) {
        int sum = 0;
        for (V4MatchSubmissionPlayerStatReq row : usedRows(rows)) {
            sum += nvl(row.getPoints());
        }
        return sum;
    }

    public static void validatePeriodScores(String gameLabel, List<V4MatchSubmissionPeriodScoreReq> periods,
                                            int homeScore, int awayScore, boolean requireComplete) {
        validatePeriodScores(gameLabel, periods, homeScore, awayScore, requireComplete, true);
    }

    public static void validatePeriodScores(String gameLabel, List<V4MatchSubmissionPeriodScoreReq> periods,
                                            int homeScore, int awayScore, boolean requireComplete,
                                            boolean requirePeriods) {
        if (!requirePeriods) {
            return;
        }
        if (!requireComplete && (periods == null || periods.isEmpty())) {
            return;
        }
        if (periods == null || periods.isEmpty()) {
            throw new ServiceException(gameLabel + "：请填写节比分");
        }
        int home = 0;
        int away = 0;
        Set<Integer> seen = new HashSet<>();
        for (V4MatchSubmissionPeriodScoreReq period : periods) {
            if (period == null || period.getPeriodNo() == null) {
                continue;
            }
            if (!seen.add(period.getPeriodNo())) {
                throw new ServiceException(gameLabel + "：节次 " + period.getPeriodNo() + " 重复");
            }
            assertNonNegative(gameLabel + " · Q" + period.getPeriodNo(), "主队得分", nvl(period.getHomeScore()));
            assertNonNegative(gameLabel + " · Q" + period.getPeriodNo(), "客队得分", nvl(period.getAwayScore()));
            home += nvl(period.getHomeScore());
            away += nvl(period.getAwayScore());
        }
        if (requireComplete && home != homeScore) {
            throw new ServiceException(gameLabel + "：节比分主队合计 " + home + " 必须等于本场比分 " + homeScore);
        }
        if (requireComplete && away != awayScore) {
            throw new ServiceException(gameLabel + "：节比分客队合计 " + away + " 必须等于本场比分 " + awayScore);
        }
    }

    /**
     * 球队总分必须等于球员 PTS 合计。0 分是合法结果（例如 0:21），
     * 只要公式成立且不是平局。0:0 仍按现有“不允许平局”拒绝。
     */
    public static void validateScoreCross(String gameLabel, int homeScore, int awayScore,
                                          int homePtsSum, int awayPtsSum) {
        if (homeScore < 0 || awayScore < 0) {
            throw new ServiceException(gameLabel + "：双方比分必须填写且不能为负数");
        }
        if (homePtsSum != homeScore) {
            throw new ServiceException(gameLabel + "：主队球员 PTS 合计 " + homePtsSum + " 必须等于主队比分 " + homeScore);
        }
        if (awayPtsSum != awayScore) {
            throw new ServiceException(gameLabel + "：客队球员 PTS 合计 " + awayPtsSum + " 必须等于客队比分 " + awayScore);
        }
        if (homeScore == awayScore) {
            throw new ServiceException(gameLabel + "：比赛不允许平局");
        }
    }

    public static void validateMaterials(String gameLabel, List<Long> materialFileIds, boolean requireComplete) {
        if (materialFileIds != null && materialFileIds.size() > 9) {
            throw new ServiceException(gameLabel + "：比赛截图最多上传 9 张");
        }
        if (requireComplete && (materialFileIds == null || materialFileIds.isEmpty())) {
            throw new ServiceException(gameLabel + "：请上传本场证明材料");
        }
    }

    public static List<V4MatchSubmissionPlayerStatReq> usedRows(List<V4MatchSubmissionPlayerStatReq> rows) {
        List<V4MatchSubmissionPlayerStatReq> used = new ArrayList<>();
        if (rows == null) {
            return used;
        }
        for (V4MatchSubmissionPlayerStatReq row : rows) {
            if (row == null) {
                continue;
            }
            if (row.getEventPlayerId() != null || hasAnyStat(row)) {
                used.add(row);
            }
        }
        return used;
    }

    public static boolean hasAnyStat(V4MatchSubmissionPlayerStatReq row) {
        return nvl(row.getPoints()) > 0
                || nvl(row.getRebound()) > 0
                || nvl(row.getAssist()) > 0
                || nvl(row.getSteal()) > 0
                || nvl(row.getBlock()) > 0
                || nvl(row.getFoul()) > 0
                || nvl(row.getTurnover()) > 0
                || nvl(row.getFieldGoalMade()) > 0
                || nvl(row.getFieldGoalAttempt()) > 0
                || nvl(row.getThreePointMade()) > 0
                || nvl(row.getThreePointAttempt()) > 0
                || nvl(row.getFreeThrowMade()) > 0
                || nvl(row.getFreeThrowAttempt()) > 0;
    }

    private static void assertNonNegative(String prefix, String field, int value) {
        if (value < 0) {
            throw new ServiceException(prefix + "：" + field + " 必须为大于等于 0 的整数");
        }
    }

    public static String formatPrefixForTeam(String gameLabel, String teamName) {
        return formatPrefix(gameLabel, teamName, null, 0);
    }

    private static String formatPrefix(String gameLabel, String teamName, V4MatchSubmissionPlayerStatReq row, int index) {
        String game = gameLabel == null || gameLabel.isBlank() ? "本场" : gameLabel;
        String team = teamName == null || teamName.isBlank() ? "球队" : teamName;
        String player = row != null && row.getPlayerName() != null && !row.getPlayerName().isBlank()
                ? row.getPlayerName()
                : (index > 0 ? "第 " + index + " 行" : "");
        if (player.isEmpty()) {
            return game + " · " + team;
        }
        return game + " · " + team + " · " + player;
    }
}
