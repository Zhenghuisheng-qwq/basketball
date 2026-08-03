package com.zhs.response.v4;

import lombok.Data;
import java.util.Date;

@Data
public class V4MyMatchResp {
    private Long matchId;
    private Long seasonId;
    private String seasonTitle;
    private Long divisionId;
    private String divisionName;
    private String platformCode;
    private Integer roundNo;
    private String matchNo;
    private Date matchTime;
    private Long homeEventTeamId;
    private String homeTeamName;
    private Long homeTeamLogoFileId;
    private Long awayEventTeamId;
    private String awayTeamName;
    private Long awayTeamLogoFileId;
    private Long myEventTeamId;
    private Long opponentEventTeamId;
    private String matchStatus;
    private String publishStatus;
    private String mySubmissionStatus;
    private Long myLatestSubmissionId;
    private Boolean canSubmit;
    private String cannotSubmitReason;
}
