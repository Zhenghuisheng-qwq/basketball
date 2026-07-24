package com.zhs.request.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("更新球队成员请求")
public class TeamMemberUpdateReq implements Serializable {

    @ApiModelProperty("球衣号码")
    private String jerseyNo;

    @ApiModelProperty("阵容类型：OFFICIAL / TRIAL")
    private String rosterType;

    @ApiModelProperty("备注")
    private String remark;
}
