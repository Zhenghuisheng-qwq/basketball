package com.zhs.response.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@ApiModel("球队资料编辑页数据")
public class TeamProfileEditResp implements Serializable {

    @ApiModelProperty("正式球队资料")
    private TeamDetailResp officialProfile;

    @ApiModelProperty("当前待审核变更申请")
    private TeamProfileChangeDetailResp pendingChange;

    @ApiModelProperty("最近变更记录")
    private List<TeamProfileChangeItemResp> recentHistory;

    @ApiModelProperty("是否可提交变更")
    private Boolean canSubmitChange;

    @ApiModelProperty("是否有待审核申请")
    private Boolean hasPendingChange;

    @ApiModelProperty("当前用户是否为队长")
    private Boolean isCaptain;
}
