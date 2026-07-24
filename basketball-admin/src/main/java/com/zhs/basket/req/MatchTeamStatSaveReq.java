package com.zhs.basket.req;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class MatchTeamStatSaveReq {
    private List<TeamStatItem> stats;

    @Data
    public static class TeamStatItem {
        private Long teamId;
        private Integer points;
        private Integer rebounds;
        private Integer offensiveRebounds;
        private Integer defensiveRebounds;
        private Integer assists;
        private Integer steals;
        private Integer blocks;
        private Integer turnovers;
        private Integer fouls;
        private Integer fieldGoalsMade;
        private Integer fieldGoalsAttempted;
        private Integer threePointsMade;
        private Integer threePointsAttempted;
        private Integer freeThrowsMade;
        private Integer freeThrowsAttempted;
        private BigDecimal possessions;
        private BigDecimal efficiency;
    }
}
