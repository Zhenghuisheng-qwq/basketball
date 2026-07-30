package com.zhs.response.matchdata;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("比赛异常申请可提交信息")
public class PlayerMatchExceptionAvailableResp implements Serializable {

    private String matchId;
    private Boolean canSubmitException;
    private String cannotReason;
    private String currentExceptionId;
    private String currentExceptionStatus;
    private String homeTeamName;
    private String awayTeamName;
    private Integer homeScore;
    private Integer awayScore;
    private Integer dataStatus;
    private String dataStatusText;
}
