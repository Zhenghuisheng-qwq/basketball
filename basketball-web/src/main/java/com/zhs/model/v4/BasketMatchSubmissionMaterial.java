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
 * CNPA 赛事域 V4 实体：BasketMatchSubmissionMaterial
 */
@Data
@ApiModel
@TableName("basket_match_submission_material")
public class BasketMatchSubmissionMaterial implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long submissionId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long matchId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long divisionId;

    private String materialType;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long fileId;

    private String fileName;

    private String remark;

    private Date createTime;

    private Date updateTime;

    private Integer isDelete;

}
