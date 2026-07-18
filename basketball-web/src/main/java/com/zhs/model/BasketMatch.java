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
 * 比赛赛程�?
 *
 * @author 
 * @since 2026-07-18
 */

@Data
@ApiModel
public class BasketMatch implements Serializable {

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
    @ApiModelProperty(value = "轮次编号")
    private Long roundId;


    @ApiModelProperty(value = "比赛编号")
    private String matchNo;

    @ApiModelProperty(value = "比赛名称")
    private String matchName;

    @ApiModelProperty(value = "比赛时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date matchTime;


    @ApiModelProperty(value = "房间名称")
    private String roomName;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主队编号")
    private Long homeTeamId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "客队编号")
    private Long awayTeamId;


    @ApiModelProperty(value = "主队得分")
    private Integer homeScore;

    @ApiModelProperty(value = "客队得分")
    private Integer awayScore;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "胜方球队编号")
    private Long winnerTeamId;


    @ApiModelProperty(value = "比赛状态")
    private String matchStatus;

    @ApiModelProperty(value = "数据状态")
    private String dataStatus;

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


    @ApiModelProperty(value = "备注")
    private String remark;



    @ApiModelProperty(value = "逻辑删除：0未删 1已删")
    private Integer isDelete;

//===========================自定义字段=================================

}
