package com.zhs.response.team;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhs.response.common.PlatformGameIdItemResp;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 队内成员私有详情（需登录且同队权限）。
 */
@Data
@ApiModel("队内成员详情")
public class TeamMemberDetailResp implements Serializable {

    @ApiModelProperty("球队成员主键")
    private String teamMemberId;

    @ApiModelProperty("用户主键")
    private String memberId;

    @ApiModelProperty("用户ID")
    private String userId;

    @ApiModelProperty("球员档案主键")
    private String playerId;

    @ApiModelProperty("永久球员编号")
    private String playerNo;

    @ApiModelProperty("昵称")
    private String nickname;

    @ApiModelProperty("头像 URL")
    private String avatarUrl;

    @ApiModelProperty("场上位置编码")
    private String positionCode;

    @ApiModelProperty("场上位置名称")
    private String positionName;

    @ApiModelProperty("球队角色")
    private String roleInTeam;

    @ApiModelProperty("球队角色文案")
    private String roleInTeamText;

    @ApiModelProperty("成员状态")
    private Integer memberStatus;

    @ApiModelProperty("成员状态文案")
    private String memberStatusText;

    @ApiModelProperty("阵容类型")
    private String rosterType;

    @ApiModelProperty("阵容类型文案")
    private String rosterTypeText;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("加入时间")
    private Date joinTime;

    @ApiModelProperty("球衣号")
    private String jerseyNo;

    @ApiModelProperty("有效平台游戏ID列表")
    private List<PlatformGameIdItemResp> platformGameIds = Collections.emptyList();

    @ApiModelProperty("游戏ID摘要")
    private String gameIdSummary;

    @ApiModelProperty("球队主键")
    private String teamId;

    @ApiModelProperty("球队名称")
    private String teamName;

    @ApiModelProperty("目标成员是否队长")
    private Boolean isCaptain;

    @ApiModelProperty("当前登录用户是否可管理该成员（队长）")
    private Boolean canManage;

    @ApiModelProperty("当前阵容/成员状态文案")
    private String currentRosterStatus;

    @ApiModelProperty("加入来源")
    private String sourceType;

    @ApiModelProperty("加入来源文案")
    private String sourceTypeText;

    @ApiModelProperty("参赛赛事数（本轮可为空）")
    private Integer participatedEventCount;

    @ApiModelProperty("当前赛事名称（本轮可为空）")
    private String currentEventName;

    @ApiModelProperty("当前赛事平台（本轮可为空）")
    private String currentEventPlatform;

    @ApiModelProperty("最近比赛时间（本轮可为空）")
    private String latestMatchTime;

    @ApiModelProperty("最近比赛摘要（本轮可为空）")
    private String latestMatchSummary;

    @ApiModelProperty("总场次（本轮可为空）")
    private Integer totalMatches;

    @ApiModelProperty("总得分（本轮可为空）")
    private Integer totalPoints;

    @ApiModelProperty("总篮板（本轮可为空）")
    private Integer totalRebounds;

    @ApiModelProperty("总助攻（本轮可为空）")
    private Integer totalAssists;

    @ApiModelProperty("场均得分（本轮可为空）")
    private String avgPoints;

    @ApiModelProperty("场均篮板（本轮可为空）")
    private String avgRebounds;

    @ApiModelProperty("场均助攻（本轮可为空）")
    private String avgAssists;

    @ApiModelProperty("最近比赛列表（本轮可为空）")
    private List<?> recentMatches = Collections.emptyList();

    @ApiModelProperty("审核记录（本轮可为空）")
    private List<?> auditRecords = Collections.emptyList();

    @ApiModelProperty("处罚记录（本轮可为空）")
    private List<?> punishmentRecords = Collections.emptyList();
}
