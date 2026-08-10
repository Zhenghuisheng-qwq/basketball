package com.zhs.response.team;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import com.zhs.response.common.PlatformGameIdItemResp;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@ApiModel("球队成员项")
public class TeamMemberItemResp implements Serializable {

    @ApiModelProperty("球队成员主键")
    private String teamMemberId;

    @ApiModelProperty("用户主键")
    private String memberId;

    @ApiModelProperty("用户ID")
    private String userId;

    @ApiModelProperty("球员档案主键")
    private String playerId;

    @ApiModelProperty("昵称")
    private String nickname;

    @ApiModelProperty("永久球员编号")
    private String playerNo;

    @ApiModelProperty("场上位置")
    private String positionCode;

    @ApiModelProperty("场上位置中文名")
    private String positionName;

    @ApiModelProperty("球员认证状态")
    private Integer certificationStatus;

    @ApiModelProperty("头像 URL")
    private String avatarUrl;

    @ApiModelProperty("比赛终端")
    private String gameTerminal;

    @ApiModelProperty("游戏账号")
    private String gameAccount;

    @ApiModelProperty("有效平台游戏ID列表")
    private List<PlatformGameIdItemResp> platformGameIds;

    @ApiModelProperty("游戏ID摘要，如 PC：xxx / PS：yyy")
    private String gameIdSummary;

    @ApiModelProperty("球员状态")
    private Integer playerStatus;

    @ApiModelProperty("当前球队主键")
    private String currentTeamId;

    @ApiModelProperty("成员状态文案")
    private String membershipStatusText;

    @ApiModelProperty("阵容类型文案")
    private String rosterTypeText;

    @ApiModelProperty("游戏昵称")
    private String gameNickname;

    @ApiModelProperty("角色")
    private String roleType;

    @ApiModelProperty("阵容类型")
    private String rosterType;

    @ApiModelProperty("球衣号码")
    private String jerseyNo;

    @ApiModelProperty("成员状态")
    private Integer membershipStatus;

    @ApiModelProperty("离队原因")
    private String leaveReason;

    @ApiModelProperty("备注")
    private String remark;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("加入时间")
    private Date joinTime;
}
