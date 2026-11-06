package com.zhs.request.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("绑定队长授权到历史球队")
public class CaptainGrantBindTeamReq implements Serializable {

    @ApiModelProperty("球队主键")
    private String teamId;

    @ApiModelProperty("要继承到的赛事 Division ID")
    private Long divisionId;
}
