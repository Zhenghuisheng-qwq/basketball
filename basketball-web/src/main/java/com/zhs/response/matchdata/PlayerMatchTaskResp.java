package com.zhs.response.matchdata;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("我的比赛列表项")
public class PlayerMatchTaskResp implements Serializable {

    private String matchId;
    private String matchNo;
    private String seasonId;
    private String stageId;
    private String homeTeamId;
    private String homeTeamName;
    private String awayTeamId;
    private String awayTeamName;
    private Date scheduledStartTime;
    private Integer matchStatus;
    private String matchStatusText;
    private Integer dataStatus;
    private String dataStatusText;
    private Integer homeScore;
    private Integer awayScore;
    private String myTeamId;
    private String myTeamName;
    private Boolean isCaptain;
    private Integer submissionStatus;
    private String submissionStatusText;
    private String submissionId;
    private String submissionNo;
    private Integer versionNo;
    private Boolean canSubmit;
    private Boolean canResubmit;
    private Boolean canViewStatus;
    private String actionText;
    private String statusText;
    private String rejectReason;
}
