package com.zhs.basket.req;

import lombok.Data;

@Data
public class MatchExceptionDecideReq {
    private String decisionType;
    private String decisionResult;
    private Integer decisionScoreHome;
    private Integer decisionScoreAway;
    private Long winnerTeamId;
    private Long walkoverTeamId;
    private String scheduledStartTime;
    private String auditRemark;
    private MatchOfficialDataCorrectionReq correctionData;
}
