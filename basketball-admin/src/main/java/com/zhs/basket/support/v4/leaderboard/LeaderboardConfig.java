package com.zhs.basket.support.v4.leaderboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeaderboardConfig {

    private String templateCode;
    private String rankingAudience = LeaderboardScopes.QUALIFIED_ONLY;
    private List<Long> overallStageIds = new ArrayList<>();
    private TeamRule team = new TeamRule();
    private PlayerRule player = new PlayerRule();
    private Map<String, MetricGate> metricGates = new LinkedHashMap<>();
    private boolean averageAndTotalSameEligibility = true;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TeamRule {
        private int minimumTeamGames;
        private int minimumTeamPlayedRounds;
        private int minimumTeamPlayedSeries;
        private BigDecimal minimumDueFixtureRate;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlayerRule {
        private boolean requireEligibleTeam = true;
        private int minimumPlayerGames;
        private int minimumPlayerPlayedRounds;
        private int minimumPlayerPlayedSeries;
        private BigDecimal minimumPlayerTeamRate = new BigDecimal("0.5");
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MetricGate {
        private boolean enabled;
        private int minimumAttemptsPerAppearance;
    }
}
