package com.zhs.response.v4;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class V4MatchSubmissionDetailResp {
    private Long matchId;
    private String matchNo;
    private Integer roundNo;
    private Date matchTime;
    private String seasonTitle;
    private String divisionName;
    private String platformCode;
    private String gameMode;
    private Integer lineupMinCount;
    private Integer lineupMaxCount;
    private String stageName;
    private String roundName;
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
    private V4MatchSubmissionTeamStatResp homeTeamStat;
    private V4MatchSubmissionTeamStatResp awayTeamStat;
    private List<V4MatchSubmissionPlayerStatResp> homePlayerStats;
    private List<V4MatchSubmissionPlayerStatResp> awayPlayerStats;
    private List<V4MatchSubmissionPeriodScoreResp> periodScores;
    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private List<Long> materialFileIds;
    private V4MatchSubmissionSeriesContextResp seriesContext;
    private List<V4MatchSubmissionEligiblePlayerResp> eligiblePlayers;
    private List<V4MatchSubmissionEligiblePlayerResp> homeEligiblePlayers;
    private List<V4MatchSubmissionEligiblePlayerResp> awayEligiblePlayers;
    private String submissionMode;
    private Integer draftComplete;
    private Long seriesSubmissionId;
    private Boolean canSubmit;
    private String cannotSubmitReason;
}
