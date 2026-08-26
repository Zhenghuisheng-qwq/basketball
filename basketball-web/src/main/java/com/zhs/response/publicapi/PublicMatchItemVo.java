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

@Data
@ApiModel("公开比赛列表项")
public class PublicMatchItemVo implements Serializable {

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
    @ApiModelProperty("CNPA Series 主键（来自 Season.seriesId，不是 Match.seriesId）")
    private Long seriesId;

    @ApiModelProperty("CNPA Series 编码：LEAGUE / ARENA / CUP / OPEN")
    private String seriesCode;

    @ApiModelProperty("CNPA Series 名称")
    private String seriesName;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("赛区主键")
    private Long divisionId;

    @ApiModelProperty("赛区名称")
    private String divisionName;

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
    @ApiModelProperty("官方胜者（eventTeamId）")
    private Long winnerTeamId;

    @ApiModelProperty("比赛状态：SCHEDULED / PLAYING / FINISHED / CANCELLED / FORFEITED")
    private String matchStatus;

    @ApiModelProperty("数据状态")
    private Integer dataStatus;

    @ApiModelProperty("场地名称")
    private String venueName;

    @ApiModelProperty("计划开始时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date scheduledStartTime;

    @ApiModelProperty("实际结束时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date actualEndTime;

    private String resultType;
    private String resultReason;
    private String homeResultLabel;
    private String awayResultLabel;
    private Boolean homeDisqualified;
    private Boolean awayDisqualified;
    private Integer originalHomeScore;
    private Integer originalAwayScore;
}
