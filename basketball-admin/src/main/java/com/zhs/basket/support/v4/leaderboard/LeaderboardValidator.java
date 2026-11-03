package com.zhs.basket.support.v4.leaderboard;

import com.zhs.basket.domain.v4.BasketDivisionStage;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class LeaderboardValidator {

    private LeaderboardValidator() {
    }

    public static List<String> validate(LeaderboardConfig config, BasketDivisionStage stage, boolean forReady) {
        List<String> errors = new ArrayList<>();
        if (config == null || config.getTeam() == null || config.getPlayer() == null) {
            errors.add("规则配置不完整");
            return errors;
        }
        checkNonNegative(errors, "球队最低局数", config.getTeam().getMinimumTeamGames());
        checkNonNegative(errors, "球队轮次", config.getTeam().getMinimumTeamPlayedRounds());
        checkNonNegative(errors, "球队系列赛数", config.getTeam().getMinimumTeamPlayedSeries());
        checkNonNegative(errors, "球员最低局数", config.getPlayer().getMinimumPlayerGames());
        checkNonNegative(errors, "球员轮次", config.getPlayer().getMinimumPlayerPlayedRounds());
        checkNonNegative(errors, "球员系列赛数", config.getPlayer().getMinimumPlayerPlayedSeries());
        checkRate(errors, "赛程履行率", config.getTeam().getMinimumDueFixtureRate());
        checkRate(errors, "球员出场比例", config.getPlayer().getMinimumPlayerTeamRate());
        if (config.getMetricGates() != null) {
            config.getMetricGates().forEach((code, gate) -> {
                if (gate != null && gate.getMinimumAttemptsPerAppearance() < 0) {
                    errors.add(code + " 出手门槛不能为负");
                }
            });
        }
        String audience = config.getRankingAudience();
        if (audience != null
                && !LeaderboardScopes.QUALIFIED_ONLY.equals(audience)
                && !LeaderboardScopes.ALL_PARTICIPANTS.equals(audience)) {
            errors.add("上榜范围无效");
        }
        if (LeaderboardTemplates.roundLimitsForbidden(stage)) {
            if (config.getTeam().getMinimumTeamPlayedRounds() > 0 || config.getPlayer().getMinimumPlayerPlayedRounds() > 0) {
                errors.add("循环赛或小组赛不能要求淘汰轮次");
            }
        }
        if (forReady && LeaderboardTemplates.blocksReady(stage)) {
            errors.add("双败未开放执行，此模板不能发布到正式榜");
        }
        if (forReady && stage == null && (config.getOverallStageIds() == null || config.getOverallStageIds().isEmpty())) {
            errors.add("全赛事范围必须选择至少一个阶段");
        }
        return errors;
    }

    private static void checkNonNegative(List<String> errors, String label, int value) {
        if (value < 0) {
            errors.add(label + "不能为负");
        }
    }

    private static void checkRate(List<String> errors, String label, BigDecimal value) {
        if (value == null) {
            return;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            errors.add(label + "必须在 0 到 1 之间");
        }
    }
}
