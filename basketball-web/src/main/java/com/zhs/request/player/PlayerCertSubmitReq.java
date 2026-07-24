package com.zhs.request.player;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("球员认证提交请求")
public class PlayerCertSubmitReq implements Serializable {

    @ApiModelProperty(value = "认证申请主键", required = true)
    private String applicationId;
}
