package com.zhs.basket.req;

import java.util.Date;
import lombok.Data;

@Data
public class SeasonCreateReq {
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
    private Integer publicStatus;
    private String description;
}
