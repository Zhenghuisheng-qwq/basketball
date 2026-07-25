package com.zhs.request.participation;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("转会申请请求")
public class RosterChangeTransferReq implements Serializable {

    @ApiModelProperty("赛季主键")
    private String seasonId;

    @ApiModelProperty("原赛事球队主键")
    private String sourceEventTeamId;

    @ApiModelProperty("目标赛事球队主键")
    private String targetEventTeamId;

    @ApiModelProperty("申请原因")
    private String applyReason;
}
