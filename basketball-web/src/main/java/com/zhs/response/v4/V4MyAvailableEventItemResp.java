package com.zhs.response.v4;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("我的赛事-可参与条目")
public class V4MyAvailableEventItemResp {

    @ApiModelProperty("TEAM / PERSONAL")
    private String source;

    @ApiModelProperty("球队报名可参与 Division")
    private V4AvailableDivisionResp teamItem;

    @ApiModelProperty("个人报名可参与 Division")
    private V4PersonalAvailableDivisionResp personalItem;
}
