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
 * 球队邀请表
 *
 * @author 
 * @since 2026-07-24
 */

@Data
@ApiModel
public class BasketTeamInvitation implements Serializable {
//===========================数据库字段================================

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private Long id;


    @ApiModelProperty(value = "邀请编号")
    private String invitationNo;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "球队主键")
    private Long teamId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "邀请人会员主键")
    private Long inviterMemberId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "被邀请会员主键")
    private Long inviteeMemberId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "被邀请球员主键")
    private Long inviteePlayerId;


    @ApiModelProperty(value = "邀请说明")
    private String invitationMessage;

    @ApiModelProperty(value = "邀请状态")
    private Integer invitationStatus;

    @ApiModelProperty(value = "过期时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date expireTime;


    @ApiModelProperty(value = "响应时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date respondedTime;


    @ApiModelProperty(value = "响应说明")
    private String responseRemark;

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
