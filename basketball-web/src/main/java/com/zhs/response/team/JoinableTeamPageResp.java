package com.zhs.response.team;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("可加入球队分页")
public class JoinableTeamPageResp extends Page<TeamSummaryResp> {

    @ApiModelProperty("列表为空时的汇总原因")
    private String emptyReason;

    @ApiModelProperty("当前查询平台代码")
    private String platformCode;

    @ApiModelProperty("当前平台游戏身份状态")
    private String platformIdentityStatus;

    @ApiModelProperty("当前平台已加入球队主键")
    private String currentTeamId;

    @ApiModelProperty("当前平台已加入球队名称")
    private String currentTeamName;

    @ApiModelProperty("当前平台是否已有有效球队")
    private Boolean hasActiveTeam;

    @ApiModelProperty("当前平台待审核加入申请状态")
    private Integer pendingApplicationStatus;

    @ApiModelProperty("当前平台是否允许申请加入")
    private Boolean canApply;

    @ApiModelProperty("当前平台不可申请原因")
    private String disabledReason;

    public JoinableTeamPageResp() {
    }

    public JoinableTeamPageResp(long current, long size, long total) {
        super(current, size, total);
    }
}
