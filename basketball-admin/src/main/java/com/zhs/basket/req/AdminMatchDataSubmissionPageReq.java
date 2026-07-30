package com.zhs.basket.req;

import lombok.Data;

@Data
public class AdminMatchDataSubmissionPageReq {
    private Long seasonId;
    private Long stageId;
    private Long groupId;
    private Long teamId;
    private Long matchId;
    private Integer submissionStatus;
    private String auditStatus;
}
