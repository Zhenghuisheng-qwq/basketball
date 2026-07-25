package com.zhs.request.participation;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("创建 Open 报名草稿请求")
public class OpenRegistrationDraftReq implements Serializable {

    @ApiModelProperty("赛季主键")
    private String seasonId;

    @ApiModelProperty("游戏平台")
    private String platform;

    @ApiModelProperty("常用位置")
    private String positionCode;

    @ApiModelProperty("备注")
    private String remark;
}
