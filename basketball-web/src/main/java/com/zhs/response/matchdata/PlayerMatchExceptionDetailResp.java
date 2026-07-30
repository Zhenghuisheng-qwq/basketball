package com.zhs.response.matchdata;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@ApiModel("比赛异常申请详情")
public class PlayerMatchExceptionDetailResp implements Serializable {

    private String applicationId;
    private String applicationNo;
    private String matchId;
    private String matchNo;
    private String exceptionType;
    private String exceptionTypeText;
    private String exceptionReason;
    private String applicationStatus;
    private String applicationStatusText;
    private String decisionType;
    private String decisionResult;
    private Integer decisionScoreHome;
    private Integer decisionScoreAway;
    private Date submittedTime;
    private Date processedTime;
    private String processedByName;
    private String rejectReason;
    private String homeTeamName;
    private String awayTeamName;
    private String applicantTeamId;
    private String applicantTeamName;
    private String opponentTeamName;
    private List<String> materialFileIds;
    private String remark;
}
