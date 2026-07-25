package com.zhs.request.player;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("新增平台游戏 ID 请求")
public class PlatformAccountAddReq implements Serializable {

    @ApiModelProperty(value = "游戏平台", required = true)
    private String platform;

    @ApiModelProperty(value = "游戏 ID", required = true)
    private String gameId;

    @ApiModelProperty(value = "证明材料文件 ID", required = true)
    private String proofFileId;
}
