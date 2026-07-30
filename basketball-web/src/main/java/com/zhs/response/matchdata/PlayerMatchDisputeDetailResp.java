package com.zhs.response.matchdata;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@ApiModel("比赛数据争议详情")
public class PlayerMatchDisputeDetailResp implements Serializable {

    private String disputeId;
    private String disputeNo;
    private String matchId;
    private String matchNo;
    private String submissionId;
    private String disputeType;
    private String disputeTypeText;
    private String originalValue;
    private String expectedValue;
    private Integer expectedHomeScore;
    private Integer expectedAwayScore;
    private String disputeReason;
    private String disputeStatus;
    private String disputeStatusText;
    private Date disputeDeadlineTime;
    private Long remainingSeconds;
    private Date submittedTime;
    private Date processedTime;
    private String processedByName;
    private String processResult;
    private String rejectReason;
    private String handleRemark;
    private String homeTeamName;
    private String awayTeamName;
    private Integer homeScore;
    private Integer awayScore;
    private String initiatorTeamId;
    private String initiatorTeamName;
    private List<String> materialFileIds;
    private String remark;
}
