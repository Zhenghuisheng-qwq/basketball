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
 * CNPA 赛事域 V4 实体：BasketMatchPeriodScore
 */
@Data
@ApiModel
@TableName("basket_match_period_score")
public class BasketMatchPeriodScore implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long matchId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long divisionId;

    private Integer periodNo;

    private Integer homeScore;

    private Integer awayScore;

    private Date createTime;

    private Date updateTime;

    private Integer isDelete;

}
