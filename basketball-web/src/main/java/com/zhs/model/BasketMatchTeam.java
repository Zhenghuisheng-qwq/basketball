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
 * 比赛球队表
 *
 * @author 
 * @since 2026-07-24
 */

@Data
@ApiModel
public class BasketMatchTeam implements Serializable {
//===========================数据库字段================================

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private Long id;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "比赛主键")
    private Long matchId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "球队主键")
    private Long teamId;


    @ApiModelProperty(value = "主客队类型")
    private String sideType;

    @ApiModelProperty(value = "比赛得分")
    private Integer score;

    @ApiModelProperty(value = "比赛结果")
    private String resultType;

    @ApiModelProperty(value = "种子编号")
    private Integer seedNo;

    @ApiModelProperty(value = "暂停次数")
    private Integer timeoutCount;

    @ApiModelProperty(value = "犯规总数")
    private Integer foulsTotal;

    @ApiModelProperty(value = "获胜标记")
    private Integer isWinner;

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
