package com.zhs.request.player;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("球员认证材料绑定请求")
public class PlayerCertMaterialBindReq implements Serializable {

    @ApiModelProperty(value = "认证申请主键", required = true)
    private String applicationId;

    @ApiModelProperty(value = "文件主键", required = true)
    private String fileId;

    @ApiModelProperty(value = "材料类型", required = true)
    private String materialType;
}
