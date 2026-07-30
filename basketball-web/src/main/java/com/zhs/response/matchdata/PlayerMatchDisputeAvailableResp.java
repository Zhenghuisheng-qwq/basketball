package com.zhs.response.matchdata;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("比赛数据争议可发起信息")
public class PlayerMatchDisputeAvailableResp implements Serializable {

    private String matchId;
    private Boolean canDispute;
    private String cannotReason;
    private Date deadlineTime;
    private Long remainingSeconds;
    private String currentDisputeId;
    private String currentDisputeStatus;
    private String homeTeamName;
    private String awayTeamName;
    private Integer homeScore;
    private Integer awayScore;
    private Date confirmedTime;
    private Date publicTime;
}
