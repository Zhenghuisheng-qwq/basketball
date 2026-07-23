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
 * 球队赛季档案表
 *
 * @author 
 * @since 2026-07-24
 */

@Data
@ApiModel
public class BasketTeamSeasonProfile implements Serializable {
//===========================数据库字段================================

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private Long id;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "球队主键")
    private Long teamId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "赛事赛季主键")
    private Long seasonId;


    @ApiModelProperty(value = "赛季展示名称")
    private String displayName;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "赛季球队标志文件主键")
    private Long logoFileId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "赛季队长球员主键")
    private Long captainPlayerId;


    @ApiModelProperty(value = "赛季报名状态")
    private Integer registrationStatus;

    @ApiModelProperty(value = "种子编号")
    private Integer seedNo;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "所属分组主键")
    private Long groupId;


    @ApiModelProperty(value = "最终排名")
    private Integer finalRank;

    @ApiModelProperty(value = "赛季状态")
    private Integer seasonStatus;

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
