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
 * 比赛球员数据表
 *
 * @author 
 * @since 2026-07-24
 */

@Data
@ApiModel
public class BasketMatchPlayerStat implements Serializable {
//===========================数据库字段================================

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private Long id;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "比赛主键")
    private Long matchId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "赛事赛季主键")
    private Long seasonId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "赛事阶段主键")
    private Long stageId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "球队主键")
    private Long teamId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "球员主键")
    private Long playerId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "出场名单主键")
    private Long rosterId;


    @ApiModelProperty(value = "首发标记")
    private Integer startedFlag;

    @ApiModelProperty(value = "出场秒数")
    private Integer minutesSeconds;

    @ApiModelProperty(value = "得分")
    private Integer points;

    @ApiModelProperty(value = "篮板")
    private Integer rebounds;

    @ApiModelProperty(value = "进攻篮板")
    private Integer offensiveRebounds;

    @ApiModelProperty(value = "防守篮板")
    private Integer defensiveRebounds;

    @ApiModelProperty(value = "助攻")
    private Integer assists;

    @ApiModelProperty(value = "抢断")
    private Integer steals;

    @ApiModelProperty(value = "盖帽")
    private Integer blocks;

    @ApiModelProperty(value = "失误")
    private Integer turnovers;

    @ApiModelProperty(value = "犯规")
    private Integer fouls;

    @ApiModelProperty(value = "投篮命中数")
    private Integer fieldGoalsMade;

    @ApiModelProperty(value = "投篮出手数")
    private Integer fieldGoalsAttempted;

    @ApiModelProperty(value = "投篮命中率")
    private java.math.BigDecimal fieldGoalPct;

    @ApiModelProperty(value = "三分命中数")
    private Integer threePointsMade;

    @ApiModelProperty(value = "三分出手数")
    private Integer threePointsAttempted;

    @ApiModelProperty(value = "三分命中率")
    private java.math.BigDecimal threePointPct;

    @ApiModelProperty(value = "罚球命中数")
    private Integer freeThrowsMade;

    @ApiModelProperty(value = "罚球出手数")
    private Integer freeThrowsAttempted;

    @ApiModelProperty(value = "罚球命中率")
    private java.math.BigDecimal freeThrowPct;

    @ApiModelProperty(value = "正负值")
    private Integer plusMinus;

    @ApiModelProperty(value = "效率值")
    private java.math.BigDecimal efficiency;

    @ApiModelProperty(value = "两双标记")
    private Integer doubleDouble;

    @ApiModelProperty(value = "三双标记")
    private Integer tripleDouble;

    @ApiModelProperty(value = "数据状态")
    private Integer dataStatus;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "确认人主键")
    private Long confirmedBy;


    @ApiModelProperty(value = "确认时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date confirmedTime;


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
