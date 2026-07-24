package com.zhs.request.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@ApiModel("保存赛事报名阵容请求")
public class TeamRegistrationRosterReq implements Serializable {

    @ApiModelProperty("阵容列表")
    private List<TeamRegistrationRosterItemReq> rosterItems;
}
