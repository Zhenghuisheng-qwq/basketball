package com.zhs.response.account;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("账户赛事报名项")
public class AccountTournamentItemResp implements Serializable {

    @ApiModelProperty("报名主键")
    private String registrationId;

    @ApiModelProperty("报名编号")
    private String registrationNo;

    @ApiModelProperty("赛事主键")
    private String tournamentId;

    @ApiModelProperty("赛事名称")
    private String tournamentName;

    @ApiModelProperty("赛季主键")
    private String seasonId;

    @ApiModelProperty("赛季名称")
    private String seasonName;

    @ApiModelProperty("报名平台代码")
    private String platformCode;

    @ApiModelProperty("报名平台展示名称")
    private String platformName;

    @ApiModelProperty("球队主键")
    private String teamId;

    @ApiModelProperty("球队名称")
    private String teamName;

    @ApiModelProperty("球队标志 URL")
    private String teamLogoUrl;

    @ApiModelProperty("报名状态")
    private Integer registrationStatus;

    @ApiModelProperty("报名阵容角色")
    private String rosterRole;

    @ApiModelProperty("首发标记")
    private Integer isStarting;

    @ApiModelProperty("资格校验状态")
    private Integer validationStatus;

    @ApiModelProperty("提交时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date submittedTime;

    @ApiModelProperty("赛事开始时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date tournamentStartTime;

    @ApiModelProperty("赛事结束时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date tournamentEndTime;

    @ApiModelProperty("下一场比赛（预留）")
    private Object nextMatch;
}
