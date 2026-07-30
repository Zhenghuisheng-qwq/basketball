package com.zhs.request.matchdata;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@ApiModel("比赛数据提交请求")
public class PlayerMatchDataSubmitReq implements Serializable {

    private String submitterTeamId;
    private Integer homeScore;
    private Integer awayScore;
    private List<PeriodScoreItem> periodScores;
    private List<TeamStatItem> teamStats;
    private List<PlayerStatItem> playerStats;
    private List<String> materialFileIds;
    private String remark;

    @Data
    public static class PeriodScoreItem implements Serializable {
        private Integer periodNo;
        private String periodType;
        private Integer homeScore;
        private Integer awayScore;
    }

    @Data
    public static class TeamStatItem implements Serializable {
        private String teamId;
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

    @Data
    public static class PlayerStatItem implements Serializable {
        private String teamId;
        private String playerId;
        private String rosterId;
        private Integer startedFlag;
        private Integer minutesSeconds;
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
        private Integer plusMinus;
        private BigDecimal efficiency;
    }
}
