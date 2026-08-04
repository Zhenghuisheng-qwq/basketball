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
 * CNPA 赛事域 V4 实体：BasketDivision
 */
@Data
@ApiModel
@TableName("basket_division")
public class BasketDivision implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;

    private String platformCode;

    private String gameMode;

    private String divisionName;

    private String registrationMode;

    private String formatType;

    private Long formatTemplateId;

    private Long formatSnapshotId;

    private Integer maxTeamCount;

    private Integer maxPersonCount;

    private Integer lineupMinCount;

    private Integer lineupMaxCount;

    private Date registrationStartTime;

    private Date registrationEndTime;

    private Date matchStartTime;

    private Date matchEndTime;

    private String divisionStatus;

    private Integer sortOrder;

    private Date createTime;

    private Date updateTime;

    private Integer isDelete;

}
