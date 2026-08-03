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
 * CNPA 赛事域 V4 实体：BasketV4Match
 */
@Data
@ApiModel
@TableName("basket_match")
public class BasketV4Match implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long divisionId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long scheduleBatchId;

    private Integer roundNo;

    private String matchNo;

    private String stageCode;

    private Date matchTime;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long homeEventTeamId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long awayEventTeamId;

    private Integer homeScore;

    private Integer awayScore;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long winnerEventTeamId;

    private String matchStatus;

    private String publishStatus;

    private String platformCode;

    private String venueText;

    private String remark;

    private Date createTime;

    private Date updateTime;

    private Integer isDelete;

}
