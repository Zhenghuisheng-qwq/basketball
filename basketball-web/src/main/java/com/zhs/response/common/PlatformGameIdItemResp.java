package com.zhs.response.common;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("平台游戏ID项")
public class PlatformGameIdItemResp implements Serializable {

    @ApiModelProperty("平台代码")
    private String platformCode;

    @ApiModelProperty("游戏ID")
    private String gameId;
}
