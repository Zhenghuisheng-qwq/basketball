package com.zhs.response.matchdata;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("比赛数据审核状态")
public class PlayerMatchDataStatusResp implements Serializable {

    private String matchId;
    private String matchNo;
    private String submissionId;
    private String submissionNo;
    private Integer versionNo;
    private Integer submissionStatus;
    private String submissionStatusText;
    private String auditStatus;
    private String auditStatusText;
    private Date submittedTime;
    private Date reviewedTime;
    private String reviewedByName;
    private String rejectReason;
    private String auditRemark;
    private String submitterTeamId;
    private String submitterTeamName;
    private Boolean canResubmit;
    private String statusText;
}
