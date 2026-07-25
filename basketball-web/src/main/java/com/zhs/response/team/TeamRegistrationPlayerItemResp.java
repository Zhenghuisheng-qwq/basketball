package com.zhs.response.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("赛事报名球员项")
public class TeamRegistrationPlayerItemResp implements Serializable {

    @ApiModelProperty("报名球员主键")
    private String registrationPlayerId;

    @ApiModelProperty("球队成员主键")
    private String teamMemberId;

    @ApiModelProperty("用户主键")
    private String memberId;

    @ApiModelProperty("用户ID")
    private String userId;

    @ApiModelProperty("球员档案主键")
    private String playerId;

    @ApiModelProperty("游戏昵称")
    private String gameNickname;

    @ApiModelProperty("永久球员编号")
    private String playerNo;

    @ApiModelProperty("场上位置")
    private String positionCode;

    @ApiModelProperty("阵容角色")
    private String rosterRole;

    @ApiModelProperty("阵容角色文案")
    private String rosterRoleText;

    @ApiModelProperty("球衣号码")
    private String jerseyNo;

    @ApiModelProperty("是否首发")
    private Integer isStarting;

    @ApiModelProperty("是否场上队长")
    private Integer isCaptain;

    @ApiModelProperty("校验状态")
    private Integer validationStatus;

    @ApiModelProperty("排序号")
    private Integer sortNo;
}
