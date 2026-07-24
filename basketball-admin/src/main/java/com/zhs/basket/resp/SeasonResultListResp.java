package com.zhs.basket.resp;

import lombok.Data;

@Data
public class SeasonResultListResp {
    private Long seasonId;
    private String seasonNo;
    private String seasonName;
    private Integer seasonYear;
    private Long tournamentId;
    private Integer seasonStatus;
    private String seasonStatusName;
    private Long championTeamId;
    private String championTeamName;
    private Long runnerUpTeamId;
    private String runnerUpTeamName;
    private String resultStatus;
    private String resultStatusName;
}
