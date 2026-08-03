package com.zhs.response.v4;

import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class V4MatchSubmissionDetailResp {
    private Long matchId;
    private String matchNo;
    private Integer roundNo;
    private Date matchTime;
    private String divisionName;
    private String platformCode;
    private Long homeEventTeamId;
    private String homeTeamName;
    private Long awayEventTeamId;
    private String awayTeamName;
    private Long submitEventTeamId;
    private String submitTeamName;
    private Long submissionId;
    private Integer versionNo;
    private String submissionStatus;
    private Integer homeScore;
    private Integer awayScore;
    private String submitRemark;
    private Date submitTime;
    private String auditUserName;
    private Date auditTime;
    private String auditRemark;
    private String rejectReason;
    private V4MatchSubmissionTeamStatResp teamStat;
    private List<V4MatchSubmissionPlayerStatResp> playerStats;
    private List<Long> materialFileIds;
}
