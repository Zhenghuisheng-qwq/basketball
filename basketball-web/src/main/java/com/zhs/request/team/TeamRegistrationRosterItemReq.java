package com.zhs.request.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("赛事报名阵容项")
public class TeamRegistrationRosterItemReq implements Serializable {

    @ApiModelProperty("球队成员主键")
    private String teamMemberId;

    @ApiModelProperty("阵容角色")
    private String rosterRole;

    @ApiModelProperty("球衣号码")
    private String jerseyNo;

    @ApiModelProperty("是否首发：0 否，1 是")
    private Integer isStarting;

    @ApiModelProperty("是否场上队长：0 否，1 是")
    private Integer isCaptain;

    @ApiModelProperty("排序号")
    private Integer sortNo;
}
