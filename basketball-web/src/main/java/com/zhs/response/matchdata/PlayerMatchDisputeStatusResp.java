package com.zhs.response.matchdata;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("比赛数据争议状态")
public class PlayerMatchDisputeStatusResp implements Serializable {

    private String matchId;
    private String matchNo;
    private String disputeId;
    private String disputeNo;
    private String disputeType;
    private String disputeTypeText;
    private String disputeStatus;
    private String disputeStatusText;
    private Date submittedTime;
    private Date disputeDeadlineTime;
    private Long remainingSeconds;
    private Date processedTime;
    private String processedByName;
    private String processResult;
    private String rejectReason;
    private String handleRemark;
    private String statusText;
    private Boolean canDispute;
    private String cannotDisputeReason;
}
