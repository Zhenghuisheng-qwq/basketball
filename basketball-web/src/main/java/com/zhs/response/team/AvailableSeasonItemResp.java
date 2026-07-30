package com.zhs.response.team;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@ApiModel("可报名赛事项")
public class AvailableSeasonItemResp implements Serializable {

    @ApiModelProperty("运营赛季主键")
    private String seasonId;

    @ApiModelProperty("运营执行层主键（同 seasonId）")
    private String operationalSeasonId;

    @ApiModelProperty("运营赛季编号")
    private String seasonNo;

    @ApiModelProperty("顶层赛季展示名称，如 2026赛季")
    private String seasonName;

    @ApiModelProperty("运营赛季名称（如常规赛阶段名）")
    private String operationalSeasonName;

    @ApiModelProperty("赛事类型编码")
    private String eventType;

    @ApiModelProperty("赛事类型名称")
    private String eventTypeName;

    @JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
    @ApiModelProperty("所属顶层赛季主键")
    private Long competitionSeasonId;

    @ApiModelProperty("所属顶层赛季名称")
    private String competitionSeasonName;

    @ApiModelProperty("赛事主键")
    private String tournamentId;

    @ApiModelProperty("赛事编号")
    private String tournamentNo;

    @ApiModelProperty("赛事名称")
    private String tournamentName;

    @ApiModelProperty("比赛终端")
    private String gameTerminal;

    @ApiModelProperty("赛事状态")
    private Integer tournamentStatus;

    @ApiModelProperty("运营赛季状态")
    private Integer seasonStatus;

    @ApiModelProperty("报名状态：NOT_STARTED/OPEN/CLOSED/ENDED")
    private String registrationStatus;

    @ApiModelProperty("赛事状态编码")
    private String eventStatus;

    @ApiModelProperty("赛事状态展示文案")
    private String eventStatusName;

    @ApiModelProperty("球队主键")
    private String teamId;

    @ApiModelProperty("当前用户所属球队主键")
    private String userTeamId;

    @ApiModelProperty("球队名称")
    private String teamName;

    @ApiModelProperty("当前用户所属球队名称")
    private String userTeamName;

    @ApiModelProperty("球队认证状态")
    private Integer teamCertificationStatus;

    @ApiModelProperty("是否可报名")
    private Boolean canRegister;

    @ApiModelProperty("不可报名原因")
    private String cannotRegisterReason;

    @ApiModelProperty("是否已报名")
    private Boolean alreadyRegistered;

    @ApiModelProperty("报名审核状态")
    private Integer auditStatus;

    @ApiModelProperty("最小阵容人数")
    private Integer minRosterCount;

    @ApiModelProperty("最大阵容人数")
    private Integer maxRosterCount;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("报名开始时间")
    private Date registrationStartTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("报名开始时间（别名 signupStartTime）")
    private Date signupStartTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("报名结束时间")
    private Date registrationEndTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("报名结束时间（别名 signupEndTime）")
    private Date signupEndTime;

    @ApiModelProperty("是否已存在报名")
    private Boolean hasRegistration;

    @ApiModelProperty("已有报名主键")
    private String registrationId;

    @ApiModelProperty("可报名参赛端编码")
    private String platformCode;

    @ApiModelProperty("可报名参赛端名称")
    private String platformName;

    @ApiModelProperty("是否可提醒队长")
    private Boolean canRemindCaptain;

    @ApiModelProperty("不可提醒原因")
    private String cannotRemindReason;

    @ApiModelProperty("24小时内是否已提醒")
    private Boolean remindedRecently;

    @ApiModelProperty("开放参赛端")
    private List<String> openedPlatforms;

    @ApiModelProperty("已报名参赛端")
    private List<String> registeredPlatforms;

    @ApiModelProperty("仍可报名参赛端")
    private List<String> availableRegisterPlatforms;

    @ApiModelProperty("我的阵容状态：SELECTED / NOT_SELECTED / NO_ROSTER")
    private String userRosterStatus;

    @ApiModelProperty("我的阵容状态文案")
    private String userRosterStatusText;

    @ApiModelProperty("我的阵容状态（别名 myRosterStatus）")
    private String myRosterStatus;

    @ApiModelProperty("我的阵容状态文案（别名 myRosterStatusName）")
    private String myRosterStatusName;

    @ApiModelProperty("当前用户是否对该赛事有有效队长授权")
    private Boolean hasActiveCaptainGrantForEvent;

    @ApiModelProperty("当前用户是否具备该赛事队长授权（别名 hasCaptainGrant）")
    private Boolean hasCaptainGrant;

    @ApiModelProperty("队长授权状态编码")
    private String captainGrantStatus;

    @ApiModelProperty("队长授权状态文案")
    private String captainGrantStatusText;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("队长授权过期时间")
    private Date captainGrantExpireTime;

    @ApiModelProperty("球队报名状态文案")
    private String teamRegistrationStatusText;

    @ApiModelProperty("球队报名状态编码")
    private String teamRegistrationStatus;

    @ApiModelProperty("球队报名状态文案（别名 teamRegistrationStatusName）")
    private String teamRegistrationStatusName;

    @ApiModelProperty("当前用户我的状态文案（未加入球队等）")
    private String userMembershipStatusText;

    @ApiModelProperty("不可操作原因（同 cannotRegisterReason）")
    private String disabledReason;

    @ApiModelProperty("用户球队角色")
    private String userTeamRole;

    @ApiModelProperty("用户在球队中的角色（别名 userRoleInTeam）")
    private String userRoleInTeam;

    @ApiModelProperty("当前用户是否为队长")
    private Boolean isCaptain;

    @ApiModelProperty("操作编码")
    private String actionCode;

    @ApiModelProperty("操作按钮文案")
    private String actionText;

    @ApiModelProperty("操作类型（别名 operationType）")
    private String operationType;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("比赛开始时间")
    private Date matchStartTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("比赛结束时间")
    private Date matchEndTime;
}
