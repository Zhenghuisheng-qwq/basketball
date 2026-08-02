package com.zhs.basket.vo;

import lombok.Data;

@Data
public class MatchDataSubmissionTeamSideVo {
    private Long eventTeamId;
    private Long teamId;
    private String teamName;
    private String teamCode;
    private String logoUrl;
    private String sideType;
    private String sideText;
    private Integer homeScore;
    private Integer awayScore;
    private String submissionStatus;
    private String submissionStatusText;
    private Integer submissionVersion;
    private Long submissionId;
    private Boolean currentSubmissionSide;
}
