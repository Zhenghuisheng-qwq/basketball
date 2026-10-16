package com.zhs.response.v4;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class V4RegistrationDivisionContextResp {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long divisionId;
    private String seriesName;
    private String seasonTitle;
    private String divisionName;
    private String platformCode;
    private String registrationMode;
    private String gameMode;
    private String divisionStatus;
    private Integer lineupMinCount;
    private Integer lineupMaxCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date registrationStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date registrationEndTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date matchStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date matchEndTime;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long grantTeamId;
    private String grantTeamName;
    private Integer maxTeamCount;
    private Integer approvedTeamCount;
    private Integer remainingTeamCount;
    private String myGrantStatus;
    private String myRegistrationStatus;
    private String participationRole;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long myRegistrationId;
    private Boolean canRegister;
    private Boolean canViewRegistration;
    private String cannotRegisterReason;
    private String cannotRegisterReasonCode;
    private String registrationWindowStatus;
    private String registrationWindowStatusLabel;
    private Boolean divisionExists;

    /** 球队本身是否满足报名入口条件（grant/team/cert），与阵容人数无关 */
    private Boolean teamEligible;
    private Integer teamMemberCount;
    private Integer eligibleRosterCount;
    private Integer ineligibleRosterCount;
    /** 入口资格 + 当前符合资格人数是否已达最低阵容（不含用户勾选） */
    private Boolean canSubmitRegistration;
    /** 已审核名单里仍然在队的人数，不含已离队球员 */
    private Integer currentEventRosterCount;
    /** ELIGIBLE / INELIGIBLE / NEEDS_REVIEW */
    private String eventEligibilityStatus;
    /** 最终参赛名单已锁定后、开赛前，才允许申请名单变更 */
    private Boolean rosterChangeAllowed;
    /** 最终参赛名单版本已锁定。与报名 APPROVED、规则 LOCKED 不是同一件事 */
    private Boolean rosterLocked;
    private List<V4RegistrationRosterPlayerResp> eligiblePlayers = new ArrayList<>();
    private List<V4RegistrationRosterPlayerResp> ineligiblePlayers = new ArrayList<>();
}
