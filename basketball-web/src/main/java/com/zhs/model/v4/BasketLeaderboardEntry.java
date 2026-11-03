package com.zhs.model.v4;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("basket_leaderboard_entry")
public class BasketLeaderboardEntry implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long runId;
    private Long snapshotId;
    private Long divisionId;
    private String scopeKey;
    private String subjectType;
    private Long eventTeamId;
    private Long eventPlayerId;
    private Integer eligible;
    private String failedConditions;
    private Integer validGames;
    private Integer playedRounds;
    private Integer playedSeries;
    private BigDecimal dueFixtureRate;
    private Integer requiredGames;
    private Integer rankNo;
    private Integer points;
    private Integer rebound;
    private Integer assist;
    private Integer steal;
    private Integer block;
    private Integer turnover;
    private Integer wins;
    private Integer losses;
    private Integer fieldGoalMade;
    private Integer fieldGoalAttempt;
    private Integer threePointMade;
    private Integer threePointAttempt;
    private Integer freeThrowMade;
    private Integer freeThrowAttempt;
    private Date createTime;
}
