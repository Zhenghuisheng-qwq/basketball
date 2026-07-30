package com.zhs.basket.vo;

import lombok.Data;

import java.util.Date;

@Data
public class MatchDataSubmissionVersionVo {
    private Long submissionId;
    private Integer versionNo;
    private Integer submissionStatus;
    private String submissionStatusName;
    private String auditStatus;
    private String submitterTeamName;
    private String submitterMemberName;
    private Date submittedTime;
    private Date reviewedTime;
    private String rejectReason;
    private Boolean isCurrent;
}
