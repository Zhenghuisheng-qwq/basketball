package com.zhs.request.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("更新球队请求")
public class TeamUpdateReq implements Serializable {

    @ApiModelProperty("球队名称")
    private String teamName;

    @ApiModelProperty("球队简称")
    private String shortName;

    @ApiModelProperty("比赛终端")
    private String gameTerminal;

    @ApiModelProperty("地区编码")
    private String regionCode;

    @ApiModelProperty("地区名称")
    private String regionName;

    @ApiModelProperty("球队介绍")
    private String description;

    @ApiModelProperty("公开状态")
    private Integer publicStatus;
}
