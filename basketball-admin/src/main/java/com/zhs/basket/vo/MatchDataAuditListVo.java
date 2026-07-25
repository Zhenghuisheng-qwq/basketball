package com.zhs.basket.vo;

import lombok.Data;

import java.util.Date;

@Data
public class MatchDataAuditListVo {
    private Long matchId;
    private String matchNo;
    private Long seasonId;
    private String seasonName;
    private Long stageId;
    private String stageName;
    private Long groupId;
    private String groupName;
    private String homeTeamName;
    private String awayTeamName;
    private Integer homeScore;
    private Integer awayScore;
    private Integer matchStatus;
    private Integer dataStatus;
    private String dataStatusName;
    private Date submitTime;
    private Date scheduledStartTime;
}
