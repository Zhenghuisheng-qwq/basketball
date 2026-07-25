package com.zhs.response.account;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("赛事报名提醒通知详情")
public class MessageRegistrationRemindInfoResp implements Serializable {

    @ApiModelProperty("赛季主键")
    private String seasonId;

    @ApiModelProperty("赛事名称")
    private String tournamentName;

    @ApiModelProperty("赛季名称")
    private String seasonName;

    @ApiModelProperty("球队主键")
    private String teamId;

    @ApiModelProperty("球队名称")
    private String teamName;

    @ApiModelProperty("提醒人昵称")
    private String reminderNickname;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("报名截止时间")
    private Date registrationEndTime;

    @ApiModelProperty("是否仍可报名")
    private Boolean canRegister;

    @ApiModelProperty("是否已报名")
    private Boolean alreadyRegistered;

    @ApiModelProperty("已有报名主键")
    private String registrationId;
}
