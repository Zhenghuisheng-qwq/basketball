package com.zhs.response.team;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@ApiModel("赛事报名详情")
public class TeamRegistrationDetailResp implements Serializable {

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

    @ApiModelProperty("球队主键")
    private String teamId;

    @ApiModelProperty("球队名称")
    private String teamName;

    @ApiModelProperty("报名平台代码")
    private String platformCode;

    @ApiModelProperty("报名平台展示名称")
    private String platformName;

    @ApiModelProperty("联系人")
    private String contactName;

    @ApiModelProperty("联系电话（脱敏）")
    private String contactMobileMasked;

    @ApiModelProperty("联系邮箱（脱敏）")
    private String contactEmailMasked;

    @ApiModelProperty("报名状态")
    private Integer registrationStatus;

    @ApiModelProperty("报名状态描述")
    private String registrationStatusText;

    @ApiModelProperty("阵容人数")
    private Integer lineupCount;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("是否可编辑")
    private Boolean canEdit;

    @ApiModelProperty("是否可提交")
    private Boolean canSubmit;

    @ApiModelProperty("是否可撤回")
    private Boolean canWithdraw;

    @ApiModelProperty("是否可删除草稿")
    private Boolean canDeleteDraft;

    @ApiModelProperty("阵容是否已锁定")
    private Boolean rosterLocked;

    @ApiModelProperty("阵容锁定状态文案")
    private String rosterLockStatusText;

    @ApiModelProperty("阵容锁定提示")
    private String rosterLockedHint;

    @ApiModelProperty("是否可申请阵容变更（预留）")
    private Boolean canRequestRosterChange;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("提交时间")
    private Date submittedTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("报名截止时间")
    private Date registrationEndTime;

    @ApiModelProperty("阵容列表")
    private List<TeamRegistrationPlayerItemResp> rosterItems;
}
