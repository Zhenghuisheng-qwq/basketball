package com.zhs.basket.vo.v4;

import lombok.Data;
import java.util.Date;

@Data
public class BasketMatchSubmissionAuditListVo {
    private Long submissionId;
    private Long matchId;
    private String matchNo;
    private Integer roundNo;
    private Date matchTime;
    private String divisionName;
    private String platformCode;
    private String homeTeamName;
    private String awayTeamName;
    private String submitTeamName;
    private String submitterName;
    private String submitterRole;
    private Integer versionNo;
    private String submissionStatus;
    private Date submitTime;
    private Date auditTime;
}
