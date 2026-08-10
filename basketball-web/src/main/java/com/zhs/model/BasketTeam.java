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
 * 赛事球队（Division 级；divisionId 为空表示 Legacy 历史球队）
 *
 * @author 
 * @since 2026-07-24
 */

@Data
@ApiModel
public class BasketTeam implements Serializable {
//===========================数据库字段================================

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private Long id;


    @ApiModelProperty(value = "球队编号")
    private String teamNo;

    @ApiModelProperty(value = "球队名称")
    private String teamName;

    @ApiModelProperty(value = "球队简称")
    private String shortName;

    @ApiModelProperty(value = "英文名称")
    private String englishName;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "球队标志文件主键")
    private Long logoFileId;


    @ApiModelProperty(value = "地区编码")
    private String regionCode;

    @ApiModelProperty(value = "地区名称")
    private String regionName;

    @ApiModelProperty(value = "比赛终端（已废弃，球队本体不绑定平台，保留字段兼容历史数据）")
    private String gameTerminal;

    @ApiModelProperty(value = "球队介绍")
    private String description;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "联系人会员主键")
    private Long contactMemberId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "队长会员主键")
    private Long captainMemberId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "队长球员主键")
    private Long captainPlayerId;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "所属赛事 Division ID，NULL 表示 Legacy 历史球队")
    private Long divisionId;


    @ApiModelProperty(value = "最大成员数量")
    private Integer maxMemberCount;

    @ApiModelProperty(value = "当前成员数量")
    private Integer currentMemberCount;

    @ApiModelProperty(value = "球队状态")
    private Integer teamStatus;

    @ApiModelProperty(value = "审核状态")
    private Integer auditStatus;

    @ApiModelProperty(value = "公开状态")
    private Integer publicStatus;

    @ApiModelProperty(value = "成立日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date establishedDate;


    @ApiModelProperty(value = "解散时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date dissolvedTime;


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
