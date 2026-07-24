package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketTournamentSeason {
    private Long id;
    private String seasonNo;
    private Long tournamentId;
    private String seasonName;
    private Integer seasonYear;
    private Date registrationStartTime;
    private Date registrationEndTime;
    private Date seasonStartTime;
    private Date seasonEndTime;
    private Long venueId;
    private Integer maxTeamCount;
    private Integer minRosterCount;
    private Integer maxRosterCount;
    private Integer seasonStatus;
    private Long currentStageId;
    private Long championTeamId;
    private Long runnerUpTeamId;
    private Integer publicStatus;
    private String description;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
