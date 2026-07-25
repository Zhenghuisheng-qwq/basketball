package com.zhs.response.player;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("平台变更申请提交结果")
public class PlatformChangeSubmitResp implements Serializable {

    @ApiModelProperty("申请编号")
    private String requestNo;

    @ApiModelProperty("申请 ID")
    private String requestId;
}
