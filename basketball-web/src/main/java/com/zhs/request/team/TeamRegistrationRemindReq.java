package com.zhs.request.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("提醒队长报名请求")
public class TeamRegistrationRemindReq implements Serializable {

    @ApiModelProperty("运营赛季主键")
    private String seasonId;
}
