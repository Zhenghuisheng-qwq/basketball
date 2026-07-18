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
 * 比赛球员数据�?
 *
 * @author 
 * @since 2026-07-18
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
    @ApiModelProperty(value = "比赛编号")
    private Long matchId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "赛事编号")
    private Long tournamentId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "赛事球队编号")
    private Long tournamentTeamId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "赛事球员编号")
    private Long tournamentPlayerId;


    @ApiModelProperty(value = "球员名称")
    private String playerName;

    @ApiModelProperty(value = "首发标志")
    private String starterFlag;

    @ApiModelProperty(value = "出场分钟")
    private Integer playMinutes;

    @ApiModelProperty(value = "得分")
    private Integer points;

    @ApiModelProperty(value = "篮板")
    private Integer rebounds;

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

    @ApiModelProperty(value = "三分命中")
    private Integer threePoints;

    @ApiModelProperty(value = "罚球命中")
    private Integer freeThrows;

    @ApiModelProperty(value = "投篮命中")
    private Integer fieldGoals;

    @ApiModelProperty(value = "数据状态")
    private String statStatus;

    @ApiModelProperty(value = "创建者")
    private String createBy;

    @ApiModelProperty(value = "创建时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;


    @ApiModelProperty(value = "更新者")
    private String updateBy;

    @ApiModelProperty(value = "更新时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;




    @ApiModelProperty(value = "逻辑删除：0未删 1已删")
    private Integer isDelete;

//===========================自定义字段=================================

}
