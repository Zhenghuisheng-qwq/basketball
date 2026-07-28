package com.zhs.response.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("赛事赛季开放平台项")
public class SeasonPlatformItemResp implements Serializable {

    @ApiModelProperty("平台代码 PC/PS/XBOX")
    private String platformCode;

    @ApiModelProperty("平台展示名称")
    private String platformName;

    @ApiModelProperty("是否开放报名")
    private Boolean open;

    @ApiModelProperty("不可选原因")
    private String disabledReason;
}
