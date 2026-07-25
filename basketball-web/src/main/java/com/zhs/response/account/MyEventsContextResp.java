package com.zhs.response.account;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("我的赛事页面身份上下文")
public class MyEventsContextResp implements Serializable {

    @ApiModelProperty("是否有球队")
    private Boolean hasTeam;

    @ApiModelProperty("当前球队主键")
    private String teamId;

    @ApiModelProperty("当前球队名称")
    private String teamName;

    @ApiModelProperty("球队角色：CAPTAIN / MEMBER / NONE")
    private String teamRole;

    @ApiModelProperty("球队认证状态")
    private Integer teamCertificationStatus;

    @ApiModelProperty("球队比赛终端")
    private String gameTerminal;
}
