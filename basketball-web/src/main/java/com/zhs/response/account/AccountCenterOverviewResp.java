package com.zhs.response.account;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@ApiModel("账户中心概览")
public class AccountCenterOverviewResp implements Serializable {

    @ApiModelProperty("账户摘要")
    private AccountSummaryResp accountSummary;

    @ApiModelProperty("球员身份信息")
    private PlayerIdentityResp playerIdentity;

    @ApiModelProperty("队长资格信息")
    private CaptainQualificationResp captainQualification;

    @ApiModelProperty("当前赛事（最多 5 条）")
    private List<AccountTournamentItemResp> currentTournaments;

    @ApiModelProperty("待办任务")
    private List<PendingTaskResp> pendingTasks;

    @ApiModelProperty("最近消息（最多 5 条）")
    private List<MessageItemResp> recentMessages;

    @ApiModelProperty("未读消息数")
    private Long unreadMessageCount;
}
