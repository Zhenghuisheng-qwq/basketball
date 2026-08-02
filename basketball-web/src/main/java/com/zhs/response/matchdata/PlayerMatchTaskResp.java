package com.zhs.response.matchdata;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.zhs.response.participation.EventRosterMemberResp;

@Data
@ApiModel("我的比赛列表项")
public class PlayerMatchTaskResp implements Serializable {

    private String matchId;
    private String matchNo;
    private String seasonId;
    private String stageId;
    private String stageName;
    private String eventName;
    private String platformCode;
    private String platformName;
    /** 赛制展示，如 5v5 */
    private String matchFormatText;
    private String eventTeamId;
    private String homeTeamId;
    private String homeTeamName;
    private String awayTeamId;
    private String awayTeamName;
    private String homeEventTeamId;
    private String homeTeamNameSnapshot;
    private String awayEventTeamId;
    private String awayTeamNameSnapshot;
    private String homeLogoUrl;
    private String awayLogoUrl;
    private Integer homeWinCount;
    private Integer homeLossCount;
    private Integer awayWinCount;
    private Integer awayLossCount;
    private String homeRecordText;
    private String awayRecordText;
    private String myTeamId;
    private String myEventTeamId;
    private String myTeamName;
    private String myTeamNameSnapshot;
    private String opponentEventTeamId;
    private String opponentTeamNameSnapshot;
    private Integer roundNo;
    private String matchDate;
    private String weekday;
    private String matchTime;
    private Date scheduledStartTime;
    private Integer matchStatus;
    private String matchStatusText;
    private Integer publishStatus;
    private String publishStatusText;
    private Integer dataStatus;
    private String dataStatusText;
    private Integer homeScore;
    private Integer awayScore;
    private Boolean isCaptain;
    private Boolean isRosterMember;
    private Integer submissionStatus;
    private String submissionStatusText;
    /** 我方提交状态（与 submissionStatus 一致，便于前端区分） */
    private Integer mySubmissionStatus;
    private String mySubmissionStatusText;
    /** 对方提交状态 */
    private Integer opponentSubmissionStatus;
    private String opponentSubmissionStatusText;
    private String submissionId;
    private String submissionNo;
    private Integer versionNo;
    private Boolean canSubmit;
    private Boolean canResubmit;
    /** 是否可提交/重新提交比赛数据（前端按钮主开关） */
    private Boolean canSubmitMatchData;
    private String submitDisabledReason;
    private Boolean isMatchTimePassed;
    private Boolean isCurrentUserCaptain;
    private Boolean canViewStatus;
    private Boolean canViewData;
    private Boolean canDispute;
    private Boolean canSubmitException;
    private String actionText;
    private String statusText;
    private String rejectReason;
    private Integer homeRosterCount;
    private Integer awayRosterCount;
    private Integer myRosterCount;
    private String rosterStatusText;
    private List<EventRosterMemberResp> homeRosterList;
    private List<EventRosterMemberResp> awayRosterList;
    private List<MatchDetailPlayerStatResp> homePlayerStatList;
    private List<MatchDetailPlayerStatResp> awayPlayerStatList;
}
