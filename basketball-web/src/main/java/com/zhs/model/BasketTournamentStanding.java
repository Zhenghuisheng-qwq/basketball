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
 * 赛事积分榜表
 *
 * @author 
 * @since 2026-07-18
 */

@Data
@ApiModel
public class BasketTournamentStanding implements Serializable {

    //===========================数据库字段================================

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private Long id;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "赛事编号")
    private Long tournamentId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "分组编号")
    private Long groupId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "赛事球队编号")
    private Long tournamentTeamId;


    @ApiModelProperty(value = "球队名称")
    private String teamName;

    @ApiModelProperty(value = "排名")
    private Integer standingRank;

    @ApiModelProperty(value = "比赛场次")
    private Integer matchCount;

    @ApiModelProperty(value = "胜场")
    private Integer winCount;

    @ApiModelProperty(value = "负场")
    private Integer loseCount;

    @ApiModelProperty(value = "平场")
    private Integer drawCount;

    @ApiModelProperty(value = "总得")
    private Integer scoreFor;

    @ApiModelProperty(value = "总失")
    private Integer scoreAgainst;

    @ApiModelProperty(value = "净胜分")
    private Integer scoreDiff;

    @ApiModelProperty(value = "积分")
    private Integer points;

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
