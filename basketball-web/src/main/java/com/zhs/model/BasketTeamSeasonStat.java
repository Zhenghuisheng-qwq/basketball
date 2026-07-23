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
 * 球队赛季汇总数据表
 *
 * @author 
 * @since 2026-07-24
 */

@Data
@ApiModel
public class BasketTeamSeasonStat implements Serializable {
//===========================数据库字段================================

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private Long id;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "赛事赛季主键")
    private Long seasonId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "赛事阶段主键")
    private Long stageId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "球队主键")
    private Long teamId;


    @ApiModelProperty(value = "比赛场次")
    private Integer games;

    @ApiModelProperty(value = "胜场")
    private Integer wins;

    @ApiModelProperty(value = "负场")
    private Integer losses;

    @ApiModelProperty(value = "总得分")
    private Integer pointsFor;

    @ApiModelProperty(value = "总失分")
    private Integer pointsAgainst;

    @ApiModelProperty(value = "场均得分")
    private java.math.BigDecimal avgPoints;

    @ApiModelProperty(value = "场均失分")
    private java.math.BigDecimal avgPointsAgainst;

    @ApiModelProperty(value = "场均篮板")
    private java.math.BigDecimal avgRebounds;

    @ApiModelProperty(value = "场均助攻")
    private java.math.BigDecimal avgAssists;

    @ApiModelProperty(value = "场均抢断")
    private java.math.BigDecimal avgSteals;

    @ApiModelProperty(value = "场均盖帽")
    private java.math.BigDecimal avgBlocks;

    @ApiModelProperty(value = "场均失误")
    private java.math.BigDecimal avgTurnovers;

    @ApiModelProperty(value = "投篮命中率")
    private java.math.BigDecimal fieldGoalPct;

    @ApiModelProperty(value = "三分命中率")
    private java.math.BigDecimal threePointPct;

    @ApiModelProperty(value = "罚球命中率")
    private java.math.BigDecimal freeThrowPct;

    @ApiModelProperty(value = "胜率")
    private java.math.BigDecimal winRate;

    @ApiModelProperty(value = "最后计算时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date lastCalculatedTime;


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
