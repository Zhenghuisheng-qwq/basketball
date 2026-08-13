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
 * CNPA 赛事域 V4 实体：BasketScheduleBatch
 */
@Data
@ApiModel
@TableName("basket_schedule_batch")
public class BasketScheduleBatch implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long divisionId;

    private String batchNo;

    private String formatType;

    private String ruleCode;

    private Integer teamCount;

    private Integer matchCount;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long ruleVersionId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long rosterVersionId;

    private String structureStatus;

    private String generateType;

    private String generateStatus;

    private Integer scheduleRevision;
    private Long stageId;
    private String publishStatus;

    private Date generateTime;

    private Date publishTime;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long generatedBy;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long publishedBy;

    private String remark;

    private Date createTime;

    private Date updateTime;

    private Integer isDelete;

}
