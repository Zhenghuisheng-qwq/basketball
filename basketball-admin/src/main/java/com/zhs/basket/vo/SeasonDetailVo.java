package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class SeasonDetailVo {
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
    private String seasonStatusName;
    private String description;
}
