package com.zhs.response.matchdata;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("比赛数据争议历史项")
public class PlayerMatchDisputeHistoryItem implements Serializable {

    private String disputeId;
    private String disputeNo;
    private String disputeType;
    private String disputeTypeText;
    private String disputeStatus;
    private String disputeStatusText;
    private String initiatorTeamId;
    private String initiatorTeamName;
    private Date submittedTime;
    private Date processedTime;
    private String processResult;
    private String rejectReason;
}
