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
 * CNPA 赛事域 V4 实体：BasketPlayerStat
 */
@Data
@ApiModel
@TableName("basket_player_stat")
public class BasketPlayerStat implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long divisionId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long eventTeamId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long eventPlayerId;

    private Integer matchCount;

    private Integer points;

    private BigDecimal avgPoints;

    private Integer rebound;

    private BigDecimal avgRebound;

    private Integer assist;

    private BigDecimal avgAssist;

    private Integer steal;

    private Integer block;

    private Integer turnover;

    private Date createTime;

    private Date updateTime;

    private Integer isDelete;

}
