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

    @ApiModelProperty("会员主键")
    private String memberId;

    @ApiModelProperty("球员主键")
    private String playerId;

    @ApiModelProperty("认证姓名")
    private String realName;

    @ApiModelProperty("游戏昵称")
    private String gameNickname;

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
