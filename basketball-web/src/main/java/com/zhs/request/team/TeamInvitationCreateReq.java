package com.zhs.request.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("创建球队邀请请求")
public class TeamInvitationCreateReq implements Serializable {

    @ApiModelProperty("被邀请球员主键（优先）")
    private String inviteePlayerId;

    @ApiModelProperty("被邀请会员主键（兼容，优先使用 inviteePlayerId）")
    private String inviteeMemberId;

    @ApiModelProperty("邀请说明")
    private String invitationMessage;
}
