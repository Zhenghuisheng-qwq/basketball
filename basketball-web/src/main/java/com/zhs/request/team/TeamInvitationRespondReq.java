package com.zhs.request.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("响应球队邀请请求")
public class TeamInvitationRespondReq implements Serializable {

    @ApiModelProperty("动作：ACCEPT / REJECT")
    private String action;

    @ApiModelProperty("是否接受（兼容字段）")
    private Boolean accept;

    @ApiModelProperty("响应说明")
    private String responseRemark;
}
