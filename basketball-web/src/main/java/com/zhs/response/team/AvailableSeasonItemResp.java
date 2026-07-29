package com.zhs.response.team;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("可报名赛事项")
public class AvailableSeasonItemResp implements Serializable {

    @ApiModelProperty("运营赛季主键")
    private String seasonId;

    @ApiModelProperty("运营赛季编号")
    private String seasonNo;

    @ApiModelProperty("运营赛季名称")
    private String seasonName;

    @JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
    @ApiModelProperty("所属顶层赛季主键")
    private Long competitionSeasonId;

    @ApiModelProperty("所属顶层赛季名称")
    private String competitionSeasonName;

    @ApiModelProperty("赛事主键")
    private String tournamentId;

    @ApiModelProperty("赛事编号")
    private String tournamentNo;

    @ApiModelProperty("赛事名称")
    private String tournamentName;

    @ApiModelProperty("比赛终端")
    private String gameTerminal;

    @ApiModelProperty("赛事状态")
    private Integer tournamentStatus;

    @ApiModelProperty("运营赛季状态")
    private Integer seasonStatus;

    @ApiModelProperty("报名状态：NOT_STARTED/OPEN/CLOSED/ENDED")
    private String registrationStatus;

    @ApiModelProperty("球队主键")
    private String teamId;

    @ApiModelProperty("球队名称")
    private String teamName;

    @ApiModelProperty("球队认证状态")
    private Integer teamCertificationStatus;

    @ApiModelProperty("是否可报名")
    private Boolean canRegister;

    @ApiModelProperty("不可报名原因")
    private String cannotRegisterReason;

    @ApiModelProperty("是否已报名")
    private Boolean alreadyRegistered;

    @ApiModelProperty("报名审核状态")
    private Integer auditStatus;

    @ApiModelProperty("最小阵容人数")
    private Integer minRosterCount;

    @ApiModelProperty("最大阵容人数")
    private Integer maxRosterCount;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("报名开始时间")
    private Date registrationStartTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("报名结束时间")
    private Date registrationEndTime;

    @ApiModelProperty("是否已存在报名")
    private Boolean hasRegistration;

    @ApiModelProperty("已有报名主键")
    private String registrationId;

    @ApiModelProperty("可报名参赛端编码")
    private String platformCode;

    @ApiModelProperty("可报名参赛端名称")
    private String platformName;

    @ApiModelProperty("是否可提醒队长")
    private Boolean canRemindCaptain;

    @ApiModelProperty("不可提醒原因")
    private String cannotRemindReason;

    @ApiModelProperty("24小时内是否已提醒")
    private Boolean remindedRecently;
}
