package com.zhs.response.team;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("球队赛事记录项（队员只读）")
public class TeamEventRecordItemResp implements Serializable {

    @ApiModelProperty("报名主键")
    private String registrationId;

    @ApiModelProperty("赛季主键")
    private String seasonId;

    @ApiModelProperty("赛事主键")
    private String tournamentId;

    @ApiModelProperty("赛事名称")
    private String tournamentName;

    @ApiModelProperty("赛季名称")
    private String seasonName;

    @ApiModelProperty("报名状态码")
    private Integer registrationStatus;

    @ApiModelProperty("报名状态描述")
    private String registrationStatusText;

    @ApiModelProperty("审核状态描述")
    private String auditStatusText;

    @ApiModelProperty("阵容状态描述")
    private String rosterStatusText;

    @ApiModelProperty("比赛/参赛状态描述")
    private String participationStatusText;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("提交时间")
    private Date submitTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("审核时间")
    private Date auditTime;

    @ApiModelProperty("驳回原因")
    private String rejectReason;
}
