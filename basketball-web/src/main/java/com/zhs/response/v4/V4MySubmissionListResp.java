package com.zhs.response.v4;

import lombok.Data;
import java.util.Date;

@Data
public class V4MySubmissionListResp {
    private Long submissionId;
    private Long matchId;
    private String matchNo;
    private String divisionName;
    private Long submitEventTeamId;
    private String teamName;
    private Integer versionNo;
    private String submissionStatus;
    private Date submitTime;
    private Date auditTime;
    private String rejectReason;
}
