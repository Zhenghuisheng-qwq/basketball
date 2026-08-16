package com.zhs.request.v4;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class V4MatchSubmissionSaveReq {
    private Long matchId;
    private Long submitEventTeamId;
    private Integer homeScore;
    private Integer awayScore;
    private V4MatchSubmissionTeamStatReq teamStat;
    private List<V4MatchSubmissionPlayerStatReq> playerStats;
    private List<V4MatchSubmissionPlayerStatReq> homePlayerStats;
    private List<V4MatchSubmissionPlayerStatReq> awayPlayerStats;
    private List<V4MatchSubmissionPeriodScoreReq> periodScores;
    private List<Long> materialFileIds;
    private String submitRemark;
    private String submissionMode;
    private Boolean draftComplete;
}
