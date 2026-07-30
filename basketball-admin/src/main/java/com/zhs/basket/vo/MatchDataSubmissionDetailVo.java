package com.zhs.basket.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class MatchDataSubmissionDetailVo {
    private Long submissionId;
    private String submissionNo;
    private Long matchId;
    private String matchNo;
    private Long seasonId;
    private String seasonName;
    private Long stageId;
    private String stageName;
    private Long groupId;
    private String groupName;
    private Long homeTeamId;
    private String homeTeamName;
    private Long awayTeamId;
    private String awayTeamName;
    private Integer homeScore;
    private Integer awayScore;
    private Long winnerTeamId;
    private Long submitterTeamId;
    private String submitterTeamName;
    private Long submitterMemberId;
    private String submitterMemberName;
    private Long submitterPlayerId;
    private String submitterRole;
    private Integer versionNo;
    private Integer submissionStatus;
    private String submissionStatusName;
    private String auditStatus;
    private Date submittedTime;
    private Date reviewedTime;
    private String reviewedByName;
    private String rejectReason;
    private String auditRemark;
    private String remark;
    private Integer matchStatus;
    private Integer dataStatus;
    private Boolean canAudit;
    private String cannotAuditReason;
    private List<MatchDataSubmissionPeriodVo> periods;
    private List<MatchDataSubmissionTeamStatVo> teamStats;
    private List<MatchDataSubmissionPlayerStatVo> playerStats;
    private List<MatchDataSubmissionMaterialVo> materials;
    private List<MatchDataSubmissionVersionVo> versions;
    private List<MatchDataSubmissionAuditLogVo> auditLogs;
}
