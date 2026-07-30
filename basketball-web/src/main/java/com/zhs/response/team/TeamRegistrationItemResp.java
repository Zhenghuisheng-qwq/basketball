package com.zhs.response.team;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("赛事报名列表项")
public class TeamRegistrationItemResp implements Serializable {

    @ApiModelProperty("报名主键")
    private String registrationId;

    @ApiModelProperty("报名编号")
    private String registrationNo;

    @ApiModelProperty("赛季主键")
    private String seasonId;

    @ApiModelProperty("赛季名称")
    private String seasonName;

    @ApiModelProperty("赛事名称")
    private String tournamentName;

    @ApiModelProperty("报名平台代码")
    private String platformCode;

    @ApiModelProperty("报名平台展示名称")
    private String platformName;

    @ApiModelProperty("报名状态")
    private Integer registrationStatus;

    @ApiModelProperty("报名状态描述")
    private String registrationStatusText;

    @ApiModelProperty("阵容人数")
    private Integer lineupCount;

    @ApiModelProperty("是否可编辑")
    private Boolean canEdit;

    @ApiModelProperty("是否可提交")
    private Boolean canSubmit;

    @ApiModelProperty("是否可撤回")
    private Boolean canWithdraw;

    @ApiModelProperty("阵容是否已锁定")
    private Boolean rosterLocked;

    @ApiModelProperty("阵容锁定状态文案")
    private String rosterLockStatusText;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("提交时间")
    private Date submittedTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("创建时间")
    private Date createTime;
}
