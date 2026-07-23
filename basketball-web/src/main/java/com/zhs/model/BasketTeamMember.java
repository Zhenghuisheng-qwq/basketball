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
 * 球队成员表
 *
 * @author 
 * @since 2026-07-24
 */

@Data
@ApiModel
public class BasketTeamMember implements Serializable {
//===========================数据库字段================================

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private Long id;


    @ApiModelProperty(value = "成员关系编号")
    private String membershipNo;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "球队主键")
    private Long teamId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "会员主键")
    private Long memberId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "球员主键")
    private Long playerId;


    @ApiModelProperty(value = "成员角色")
    private String roleType;

    @ApiModelProperty(value = "阵容类型")
    private String rosterType;

    @ApiModelProperty(value = "球衣号码")
    private String jerseyNo;

    @ApiModelProperty(value = "加入来源")
    private String joinSource;

    @ApiModelProperty(value = "成员状态")
    private Integer membershipStatus;

    @ApiModelProperty(value = "当前成员标记")
    private Integer isCurrent;

    @ApiModelProperty(value = "加入时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date joinTime;


    @ApiModelProperty(value = "离队时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date leaveTime;


    @ApiModelProperty(value = "离队原因")
    private String leaveReason;

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
