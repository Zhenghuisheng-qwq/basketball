package com.zhs.request.player;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("修改平台游戏 ID 请求")
public class PlatformAccountChangeGameIdReq implements Serializable {

    @ApiModelProperty(value = "新游戏 ID", required = true)
    private String newGameId;

    @ApiModelProperty(value = "证明材料文件 ID", required = true)
    private String proofFileId;
}
