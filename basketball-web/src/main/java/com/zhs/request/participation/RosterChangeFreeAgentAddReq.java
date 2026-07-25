package com.zhs.request.participation;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("自由球员补员申请请求")
public class RosterChangeFreeAgentAddReq implements Serializable {

    @ApiModelProperty("赛季主键")
    private String seasonId;

    @ApiModelProperty("目标赛事球队主键")
    private String targetEventTeamId;

    @ApiModelProperty("待补员球员主键")
    private String playerId;

    @ApiModelProperty("目标身份")
    private String targetRosterRole;

    @ApiModelProperty("目标首发标记")
    private Integer targetIsStarting;

    @ApiModelProperty("目标替补标记")
    private Integer targetIsSubstitute;

    @ApiModelProperty("申请原因")
    private String applyReason;
}
