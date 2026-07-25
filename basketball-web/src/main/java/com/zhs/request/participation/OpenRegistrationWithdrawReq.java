package com.zhs.request.participation;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("撤回 Open 报名请求")
public class OpenRegistrationWithdrawReq implements Serializable {

    @ApiModelProperty("撤回原因")
    private String withdrawalReason;
}
