package com.zhs.response.publicapi;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@ApiModel("公开比赛详情")
public class PublicMatchDetailVo implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("比赛主键")
    private Long matchId;

    @ApiModelProperty("比赛编号")
    private String matchNo;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("赛季主键")
    private Long seasonId;

    @ApiModelProperty("赛季名称")
    private String seasonName;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("阶段主键")
    private Long stageId;

    @ApiModelProperty("阶段名称")
    private String stageName;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("分组主键")
    private Long groupId;

    @ApiModelProperty("分组名称")
    private String groupName;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("主队主键")
    private Long homeTeamId;

    @ApiModelProperty("主队名称")
    private String homeTeamName;

    @ApiModelProperty("主队 Logo")
    private String homeTeamLogoUrl;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("客队主键")
    private Long awayTeamId;

    @ApiModelProperty("客队名称")
    private String awayTeamName;

    @ApiModelProperty("客队 Logo")
    private String awayTeamLogoUrl;

    @ApiModelProperty("主队得分")
    private Integer homeScore;

    @ApiModelProperty("客队得分")
    private Integer awayScore;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("胜方球队主键")
    private Long winnerTeamId;

    @ApiModelProperty("比赛状态")
    private Integer matchStatus;

    @ApiModelProperty("数据状态")
    private Integer dataStatus;

    @ApiModelProperty("是否返回完整技术统计（仅已确认）")
    private Boolean statsAvailable;

    @ApiModelProperty("场地名称")
    private String venueName;

    @ApiModelProperty("计划开始时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date scheduledStartTime;

    @ApiModelProperty("实际开始时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date actualStartTime;

    @ApiModelProperty("实际结束时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date actualEndTime;

    @ApiModelProperty("球队技术统计")
    private List<PublicMatchTeamStatVo> teamStats;

    @ApiModelProperty("球员技术统计")
    private List<PublicMatchPlayerStatVo> playerStats;
}
