package com.zhs.basket.support.v4.leaderboard;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.basket.enums.v4.StageTypeEnum;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class LeaderboardTemplates {

    private LeaderboardTemplates() {
    }

    public static LeaderboardConfig forStage(BasketDivisionStage stage, List<Long> allStageIds) {
        if (stage == null) {
            return overall(allStageIds);
        }
        String format = stage.getCompetitionFormat();
        String stageType = stage.getStageType();
        if (CompetitionFormatEnum.DOUBLE_ELIMINATION.getCode().equals(format)) {
            return elimination(stage, "DOUBLE_ELIMINATION");
        }
        if (StageTypeEnum.GROUP.getCode().equals(stageType)) {
            return roundRobin("GROUP_STAGE", 2, new BigDecimal("0.70"));
        }
        if (CompetitionFormatEnum.SINGLE_ELIMINATION.getCode().equals(format)
                || StageTypeEnum.KNOCKOUT.getCode().equals(stageType)
                || StageTypeEnum.SEMI_FINAL.getCode().equals(stageType)
                || StageTypeEnum.FINAL.getCode().equals(stageType)) {
            return elimination(stage, "SINGLE_ELIMINATION");
        }
        return roundRobin("ROUND_ROBIN", 3, new BigDecimal("0.70"));
    }

    public static LeaderboardConfig overall(List<Long> stageIds) {
        LeaderboardConfig config = base("OVERALL");
        config.setOverallStageIds(stageIds == null ? new ArrayList<>() : new ArrayList<>(stageIds));
        config.getTeam().setMinimumTeamGames(4);
        config.getPlayer().setMinimumPlayerGames(4);
        return config;
    }

    public static boolean blocksReady(BasketDivisionStage stage) {
        return stage != null && CompetitionFormatEnum.DOUBLE_ELIMINATION.getCode().equals(stage.getCompetitionFormat());
    }

    public static boolean roundLimitsForbidden(BasketDivisionStage stage) {
        if (stage == null) {
            return true;
        }
        if (CompetitionFormatEnum.SINGLE_ELIMINATION.getCode().equals(stage.getCompetitionFormat())) {
            return false;
        }
        String stageType = stage.getStageType();
        return !StageTypeEnum.KNOCKOUT.getCode().equals(stageType)
                && !StageTypeEnum.SEMI_FINAL.getCode().equals(stageType)
                && !StageTypeEnum.FINAL.getCode().equals(stageType);
    }

    private static LeaderboardConfig roundRobin(String template, int games, BigDecimal rate) {
        LeaderboardConfig config = base(template);
        config.getTeam().setMinimumTeamGames(games);
        config.getTeam().setMinimumDueFixtureRate(rate);
        config.getPlayer().setMinimumPlayerGames(games);
        return config;
    }

    private static LeaderboardConfig elimination(BasketDivisionStage stage, String template) {
        LeaderboardConfig config = base(template);
        int bestOf = stage.getBestOf() == null ? 1 : stage.getBestOf();
        config.getTeam().setMinimumTeamGames(Math.max(bestOf, 1));
        config.getTeam().setMinimumTeamPlayedRounds(2);
        config.getTeam().setMinimumTeamPlayedSeries(bestOf > 1 ? 2 : 0);
        config.getPlayer().setMinimumPlayerGames(Math.max(bestOf, 1));
        config.getPlayer().setMinimumPlayerPlayedRounds(2);
        config.getPlayer().setMinimumPlayerPlayedSeries(bestOf > 1 ? 2 : 0);
        return config;
    }

    private static LeaderboardConfig base(String template) {
        LeaderboardConfig config = new LeaderboardConfig();
        config.setTemplateCode(template);
        config.setRankingAudience(LeaderboardScopes.QUALIFIED_ONLY);
        config.getPlayer().setRequireEligibleTeam(true);
        config.getPlayer().setMinimumPlayerTeamRate(new BigDecimal("0.5"));
        config.getMetricGates().put("FG_PCT", gate(false, 5));
        config.getMetricGates().put("THREE_PCT", gate(false, 2));
        config.getMetricGates().put("FT_PCT", gate(false, 1));
        return config;
    }

    private static LeaderboardConfig.MetricGate gate(boolean enabled, int perAppearance) {
        LeaderboardConfig.MetricGate gate = new LeaderboardConfig.MetricGate();
        gate.setEnabled(enabled);
        gate.setMinimumAttemptsPerAppearance(perAppearance);
        return gate;
    }
}
