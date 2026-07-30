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
 * 比赛主表
 *
 * @author 
 * @since 2026-07-24
 */

@Data
@ApiModel
public class BasketMatch implements Serializable {
//===========================数据库字段================================

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private Long id;


    @ApiModelProperty(value = "比赛编号")
    private String matchNo;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "赛事赛季主键")
    private Long seasonId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "赛事阶段主键")
    private Long stageId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "赛事轮次主键")
    private Long roundId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "赛事分组主键")
    private Long groupId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "比赛场地主键")
    private Long venueId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主队主键")
    private Long homeTeamId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "客队主键")
    private Long awayTeamId;


    @ApiModelProperty(value = "计划开始时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date scheduledStartTime;


    @ApiModelProperty(value = "计划结束时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date scheduledEndTime;


    @ApiModelProperty(value = "实际开始时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date actualStartTime;


    @ApiModelProperty(value = "实际结束时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date actualEndTime;


    @ApiModelProperty(value = "比赛状态")
    private Integer matchStatus;

    @ApiModelProperty(value = "主队得分")
    private Integer homeScore;

    @ApiModelProperty(value = "客队得分")
    private Integer awayScore;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "获胜球队主键")
    private Long winnerTeamId;


    @ApiModelProperty(value = "加时次数")
    private Integer overtimeCount;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "判胜球队主键")
    private Long walkoverTeamId;


    @ApiModelProperty(value = "取消原因")
    private String cancelReason;

    @ApiModelProperty(value = "数据状态")
    private Integer dataStatus;

    @ApiModelProperty(value = "公开状态")
    private Integer publicStatus;

    @ApiModelProperty(value = "发布状态 0草稿 1已发布 2已下架")
    private Integer publishStatus;

    @ApiModelProperty(value = "轮次号")
    private Integer roundNo;

    @ApiModelProperty(value = "平台编码")
    private String platformCode;

    @ApiModelProperty(value = "主队名称快照")
    private String homeTeamNameSnapshot;

    @ApiModelProperty(value = "客队名称快照")
    private String awayTeamNameSnapshot;

    @ApiModelProperty(value = "主队Logo快照")
    private String homeTeamLogoSnapshot;

    @ApiModelProperty(value = "客队Logo快照")
    private String awayTeamLogoSnapshot;

    @ApiModelProperty(value = "排序号")
    private Integer sortNo;

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
