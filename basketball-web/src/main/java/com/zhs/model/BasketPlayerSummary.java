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
 * 球员统计汇总表
 *
 * @author 
 * @since 2026-07-18
 */

@Data
@ApiModel
public class BasketPlayerSummary implements Serializable {

    //===========================数据库字段================================

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private Long id;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "赛季编号")
    private Long seasonId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "赛事编号")
    private Long tournamentId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "来源球员编号")
    private Long sourcePlayerId;


    @ApiModelProperty(value = "球员名称")
    private String playerName;

    @ApiModelProperty(value = "比赛场次")
    private Integer matchCount;

    @ApiModelProperty(value = "总得")
    private Integer pointsTotal;

    @ApiModelProperty(value = "总篮")
    private Integer reboundsTotal;

    @ApiModelProperty(value = "总助")
    private Integer assistsTotal;

    @ApiModelProperty(value = "总抢")
    private Integer stealsTotal;

    @ApiModelProperty(value = "总盖")
    private Integer blocksTotal;

    @ApiModelProperty(value = "场均得分")
    private java.math.BigDecimal pointsAvg;

    @ApiModelProperty(value = "场均篮板")
    private java.math.BigDecimal reboundsAvg;

    @ApiModelProperty(value = "场均助攻")
    private java.math.BigDecimal assistsAvg;

    @ApiModelProperty(value = "汇总类")
    private String summaryType;

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
