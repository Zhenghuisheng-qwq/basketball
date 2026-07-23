package com.zhs.model;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.Date;
import org.springframework.format.annotation.DateTimeFormat;
import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.alibaba.fastjson2.JSONObject;


/**
 * 比赛数据变更记录表
 *
 * @author 
 * @since 2026-07-24
 */

@Data
@ApiModel
public class BasketStatChangeRecord implements Serializable {
//===========================数据库字段================================

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private Long id;


    @ApiModelProperty(value = "业务类型")
    private String bizType;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "业务主键")
    private Long bizId;


    @ApiModelProperty(value = "变更字段")
    private String fieldName;

    @ApiModelProperty(value = "变更前值")
    private Object beforeValue;

    @ApiModelProperty(value = "变更后值")
    private Object afterValue;

    @ApiModelProperty(value = "变更原因")
    private String changeReason;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "操作人主键")
    private Long operatorId;


    @ApiModelProperty(value = "操作人名称")
    private String operatorName;

    @ApiModelProperty(value = "审批状态")
    private Integer approvalStatus;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "审批人主键")
    private Long approvedBy;


    @ApiModelProperty(value = "审批时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date approvedTime;


    @ApiModelProperty(value = "创建时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;


    @ApiModelProperty(value = "更新时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;


    @ApiModelProperty(value = "删除标记")
    private Integer isDelete;


//===========================自定义字段=================================

}
