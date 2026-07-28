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
 * 队长资格表
 *
 * @author 
 * @since 2026-07-24
 */

@Data
@ApiModel
public class BasketCaptainQualification implements Serializable {
//===========================数据库字段================================

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private Long id;


    @ApiModelProperty(value = "资格编号")
    private String qualificationNo;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "会员主键")
    private Long memberId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "球员主键")
    private Long playerId;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "赛事赛季主键（队长授权所属赛段）")
    private Long seasonId;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "绑定球队主键，未建队前可为空")
    private Long teamId;

    @ApiModelProperty(value = "资格状态")
    private Integer qualificationStatus;

    @ApiModelProperty(value = "当前有效标记")
    private Integer isCurrent;

    @ApiModelProperty(value = "线下缴费确认状态")
    private Integer paymentConfirmed;

    @ApiModelProperty(value = "线下缴费参考号")
    private String paymentReference;

    @ApiModelProperty(value = "授予原因")
    private String grantReason;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "授予人主键")
    private Long grantedBy;


    @ApiModelProperty(value = "授予时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date grantedTime;


    @ApiModelProperty(value = "生效时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validFrom;


    @ApiModelProperty(value = "失效时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validTo;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "取消人主键")
    private Long revokedBy;


    @ApiModelProperty(value = "取消时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date revokedTime;


    @ApiModelProperty(value = "取消原因")
    private String revokeReason;

    @ApiModelProperty(value = "备注")
    private String remark;

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
