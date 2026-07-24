package com.zhs.request.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("撤回赛事报名请求")
public class TeamRegistrationWithdrawReq implements Serializable {

    @ApiModelProperty("撤回原因")
    private String withdrawalReason;
}
