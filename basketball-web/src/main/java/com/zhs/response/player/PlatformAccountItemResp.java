package com.zhs.response.player;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("平台游戏 ID 列表项")
public class PlatformAccountItemResp implements Serializable {

    @ApiModelProperty("平台账号 ID")
    private String platformAccountId;

    @ApiModelProperty("游戏平台")
    private String platform;

    @ApiModelProperty("展示游戏 ID")
    private String displayGameId;

    @ApiModelProperty("账号状态")
    private Integer accountStatus;

    @ApiModelProperty("账号状态说明")
    private String accountStatusText;

    @ApiModelProperty("是否主平台")
    private Boolean primary;

    @ApiModelProperty("是否有待审核变更")
    private Boolean pendingChange;
}
