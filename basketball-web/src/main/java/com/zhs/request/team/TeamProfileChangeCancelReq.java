package com.zhs.request.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("取消球队资料变更申请")
public class TeamProfileChangeCancelReq implements Serializable {

    @ApiModelProperty("取消原因")
    private String cancelReason;
}
