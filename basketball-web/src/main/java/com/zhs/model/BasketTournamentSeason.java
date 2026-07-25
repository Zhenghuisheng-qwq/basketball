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
 * 赛事赛季表
 *
 * @author 
 * @since 2026-07-24
 */

@Data
@ApiModel
public class BasketTournamentSeason implements Serializable {
//===========================数据库字段================================

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private Long id;


    @ApiModelProperty(value = "赛季编号")
    private String seasonNo;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "赛事主键")
    private Long tournamentId;


    @ApiModelProperty(value = "赛季名称")
    private String seasonName;

    @ApiModelProperty(value = "赛季年份")
    private Integer seasonYear;

    @ApiModelProperty(value = "报名开始时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date registrationStartTime;


    @ApiModelProperty(value = "报名结束时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date registrationEndTime;


    @ApiModelProperty(value = "赛季开始时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date seasonStartTime;


    @ApiModelProperty(value = "赛季结束时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date seasonEndTime;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主场地主键")
    private Long venueId;


    @ApiModelProperty(value = "最大球队数量")
    private Integer maxTeamCount;

    @ApiModelProperty(value = "最小阵容人数")
    private Integer minRosterCount;

    @ApiModelProperty(value = "最大阵容人数")
    private Integer maxRosterCount;

    @ApiModelProperty(value = "允许球队报名")
    private Integer allowTeamRegistration;

    @ApiModelProperty(value = "允许 Open 报名")
    private Integer allowOpenRegistration;

    @ApiModelProperty(value = "允许转会")
    private Integer transferEnabled;

    @ApiModelProperty(value = "转会窗口开始")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date transferStartTime;

    @ApiModelProperty(value = "转会窗口结束")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date transferEndTime;

    @ApiModelProperty(value = "允许自由球员补员")
    private Integer freeAgentEnabled;

    @ApiModelProperty(value = "补员窗口开始")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date freeAgentStartTime;

    @ApiModelProperty(value = "补员窗口结束")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date freeAgentEndTime;

    @ApiModelProperty(value = "每队最大补员次数")
    private Integer maxFreeAgentAddPerTeam;

    @ApiModelProperty(value = "赛季状态")
    private Integer seasonStatus;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "当前阶段主键")
    private Long currentStageId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "冠军球队主键")
    private Long championTeamId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "亚军球队主键")
    private Long runnerUpTeamId;


    @ApiModelProperty(value = "公开状态")
    private Integer publicStatus;

    @ApiModelProperty(value = "赛季说明")
    private Object description;

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
