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
import java.math.BigDecimal;

/**
 * CNPA 赛事域 V4 实体：BasketV4MatchPlayerStat
 */
@Data
@ApiModel
@TableName("basket_match_player_stat")
public class BasketV4MatchPlayerStat implements Serializable {

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
    private Long eventPlayerId;

    private Integer points;

    private Integer score;

    private Integer rebound;

    private Integer assist;

    private Integer steal;

    private Integer block;

    private Integer turnover;

    private Integer foul;

    /** FGM：总运动战命中，含三分 */
    private Integer fieldGoalMade;

    /** FGA：总运动战出手，含三分 */
    private Integer fieldGoalAttempt;

    /** 3PM：三分命中，是 FGM 的子集 */
    private Integer threePointMade;

    /** 3PA：三分出手，是 FGA 的子集 */
    private Integer threePointAttempt;

    private Integer freeThrowMade;

    private Integer freeThrowAttempt;

    private BigDecimal minutes;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourceSubmissionId;

    private Date createTime;

    private Date updateTime;

    private Integer isDelete;

}
