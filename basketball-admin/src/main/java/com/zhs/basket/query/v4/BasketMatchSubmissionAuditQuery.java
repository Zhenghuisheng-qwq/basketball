package com.zhs.basket.query.v4;

import lombok.Data;

@Data
public class BasketMatchSubmissionAuditQuery {
    private Long seasonId;
    private Long divisionId;
    private Long matchId;
    private Long submitEventTeamId;
    private String submissionStatus;
    private String teamName;
    private String matchNo;
}
