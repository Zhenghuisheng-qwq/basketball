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
 * 球员赛季汇总数据表
 *
 * @author 
 * @since 2026-07-24
 */

@Data
@ApiModel
public class BasketPlayerSeasonStat implements Serializable {
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


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "球员主键")
    private Long playerId;


    @ApiModelProperty(value = "比赛场次")
    private Integer games;

    @ApiModelProperty(value = "首发场次")
    private Integer starts;

    @ApiModelProperty(value = "总出场秒数")
    private Integer minutesSeconds;

    @ApiModelProperty(value = "总得分")
    private Integer points;

    @ApiModelProperty(value = "总篮板")
    private Integer rebounds;

    @ApiModelProperty(value = "总助攻")
    private Integer assists;

    @ApiModelProperty(value = "总抢断")
    private Integer steals;

    @ApiModelProperty(value = "总盖帽")
    private Integer blocks;

    @ApiModelProperty(value = "总失误")
    private Integer turnovers;

    @ApiModelProperty(value = "总犯规")
    private Integer fouls;

    @ApiModelProperty(value = "场均出场分钟")
    private java.math.BigDecimal avgMinutes;

    @ApiModelProperty(value = "场均得分")
    private java.math.BigDecimal avgPoints;

    @ApiModelProperty(value = "场均篮板")
    private java.math.BigDecimal avgRebounds;

    @ApiModelProperty(value = "场均助攻")
    private java.math.BigDecimal avgAssists;

    @ApiModelProperty(value = "场均抢断")
    private java.math.BigDecimal avgSteals;

    @ApiModelProperty(value = "场均盖帽")
    private java.math.BigDecimal avgBlocks;

    @ApiModelProperty(value = "投篮命中率")
    private java.math.BigDecimal fieldGoalPct;

    @ApiModelProperty(value = "三分命中率")
    private java.math.BigDecimal threePointPct;

    @ApiModelProperty(value = "罚球命中率")
    private java.math.BigDecimal freeThrowPct;

    @ApiModelProperty(value = "场均效率值")
    private java.math.BigDecimal avgEfficiency;

    @ApiModelProperty(value = "两双次数")
    private Integer doubleDoubleCount;

    @ApiModelProperty(value = "三双次数")
    private Integer tripleDoubleCount;

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
