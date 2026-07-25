package com.zhs.response.participation;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("我的赛事球队摘要")
public class EventTeamSummaryResp implements Serializable {

    private String eventTeamId;
    private String eventTeamNo;
    private String teamId;
    private String teamName;
    private String shortName;
    private String seasonId;
    private String seasonName;
    private String tournamentName;
    private String gameTerminal;
    private Integer participationStatus;
    private String participationStatusText;
    private Integer rosterLockStatus;
    private String rosterLockStatusText;
    private Integer rosterCount;
    private Boolean isCaptain;
    private String rosterRole;
}
