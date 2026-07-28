package com.zhs.response.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("报名阵容候选成员")
public class RegistrationRosterCandidateItemResp implements Serializable {

    @ApiModelProperty("球队成员主键")
    private String teamMemberId;

    @ApiModelProperty("球员主键")
    private String playerId;

    @ApiModelProperty("球员编号")
    private String playerNo;

    @ApiModelProperty("游戏昵称")
    private String gameNickname;

    @ApiModelProperty("昵称")
    private String nickname;

    @ApiModelProperty("场上位置")
    private String positionCode;

    @ApiModelProperty("场上位置中文名")
    private String positionName;

    @ApiModelProperty("球衣号")
    private String jerseyNo;

    @ApiModelProperty("角色")
    private String roleType;

    @ApiModelProperty("报名平台游戏 ID")
    private String platformGameId;

    @ApiModelProperty("平台游戏身份状态")
    private String platformIdentityStatus;

    @ApiModelProperty("平台游戏身份状态文案")
    private String platformIdentityStatusText;

    @ApiModelProperty("是否可选入阵容")
    private Boolean canSelect;

    @ApiModelProperty("不可选原因")
    private String cannotSelectReason;
}
