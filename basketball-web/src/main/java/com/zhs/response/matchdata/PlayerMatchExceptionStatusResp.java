package com.zhs.response.matchdata;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("比赛异常申请状态")
public class PlayerMatchExceptionStatusResp implements Serializable {

    private String matchId;
    private String matchNo;
    private String applicationId;
    private String applicationNo;
    private String exceptionType;
    private String exceptionTypeText;
    private String applicationStatus;
    private String applicationStatusText;
    private String decisionType;
    private String decisionResult;
    private Date submittedTime;
    private Date processedTime;
    private String processedByName;
    private String rejectReason;
    private String statusText;
    private Boolean canSubmitException;
    private String cannotExceptionReason;
}
