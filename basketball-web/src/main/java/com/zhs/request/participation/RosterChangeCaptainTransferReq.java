package com.zhs.request.participation;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("队长移交申请请求")
public class RosterChangeCaptainTransferReq implements Serializable {

    @ApiModelProperty("赛事球队主键")
    private String eventTeamId;

    @ApiModelProperty("新队长球员主键")
    private String newCaptainPlayerId;

    @ApiModelProperty("申请原因")
    private String applyReason;
}
