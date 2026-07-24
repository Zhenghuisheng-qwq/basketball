package com.zhs.response.player;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("球员认证材料响应")
public class PlayerCertMaterialResp implements Serializable {

    @ApiModelProperty("材料主键")
    private String materialId;

    @ApiModelProperty("材料类型")
    private String materialType;

    @ApiModelProperty("文件主键")
    private String fileId;

    @ApiModelProperty("文件访问地址")
    private String accessUrl;

    @ApiModelProperty("材料名称")
    private String materialName;
}
