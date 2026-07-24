package com.zhs.request.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("提交加入球队申请请求")
public class TeamJoinApplicationCreateReq implements Serializable {

    @ApiModelProperty("申请原因")
    private String applyReason;
}
