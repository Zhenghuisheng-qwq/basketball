package com.zhs.request.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("处理加入球队申请请求")
public class TeamJoinApplicationProcessReq implements Serializable {

    @ApiModelProperty("动作：APPROVE / REJECT")
    private String action;

    @ApiModelProperty("是否通过（兼容字段）")
    private Boolean approve;

    @ApiModelProperty("处理说明")
    private String processRemark;
}
