package com.zhs.response.matchdata;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("比赛异常申请历史项")
public class PlayerMatchExceptionHistoryItem implements Serializable {

    private String applicationId;
    private String applicationNo;
    private String exceptionType;
    private String exceptionTypeText;
    private String applicationStatus;
    private String applicationStatusText;
    private String applicantTeamId;
    private String applicantTeamName;
    private Date submittedTime;
    private Date processedTime;
    private String decisionType;
    private String decisionResult;
    private String rejectReason;
}
