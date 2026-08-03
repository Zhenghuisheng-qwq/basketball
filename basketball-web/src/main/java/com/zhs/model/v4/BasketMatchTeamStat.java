package com.zhs.model.v4;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * CNPA 赛事域 V4 实体：BasketMatchTeamStat
 */
@Data
@ApiModel
@TableName("basket_match_team_stat")
public class BasketMatchTeamStat implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long matchId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long divisionId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long eventTeamId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long opponentEventTeamId;

    private Integer isWinner;

    private Integer score;

    private Integer pointsFor;

    private Integer pointsAgainst;

    private Integer fieldGoalMade;

    private Integer fieldGoalAttempt;

    private Integer threePointMade;

    private Integer threePointAttempt;

    private Integer freeThrowMade;

    private Integer freeThrowAttempt;

    private Integer rebound;

    private Integer assist;

    private Integer steal;

    private Integer block;

    private Integer turnover;

    private Integer foul;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourceSubmissionId;

    private Date createTime;

    private Date updateTime;

    private Integer isDelete;

}
