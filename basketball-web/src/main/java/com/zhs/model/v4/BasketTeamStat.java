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
 * CNPA 赛事域 V4 实体：BasketTeamStat
 */
@Data
@ApiModel
@TableName("basket_team_stat")
public class BasketTeamStat implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long divisionId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long eventTeamId;

    private Integer matchCount;

    private Integer winCount;

    private Integer loseCount;

    private Integer pointsFor;

    private Integer pointsAgainst;

    private BigDecimal avgPointsFor;

    private BigDecimal avgPointsAgainst;

    private Integer rebound;

    private Integer assist;

    private Integer steal;

    private Integer block;

    private Integer turnover;

    private Date createTime;

    private Date updateTime;

    private Integer isDelete;

}
