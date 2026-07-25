package com.zhs.response.participation;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("赛事球队详情")
public class EventTeamDetailResp implements Serializable {

    private String eventTeamId;
    private String eventTeamNo;
    private String teamId;
    private String teamName;
    private String shortName;
    private String description;
    private String seasonId;
    private String seasonName;
    private String tournamentId;
    private String tournamentName;
    private String gameTerminal;
    private String registrationSource;
    private Integer participationStatus;
    private String participationStatusText;
    private Integer rosterLockStatus;
    private String rosterLockStatusText;
    private Integer rosterCount;
    private Integer maxRosterCount;
    private Integer minRosterCount;
    private Boolean transferEnabled;
    private Boolean freeAgentEnabled;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date transferEndTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date freeAgentEndTime;

    private Integer freeAgentQuota;
    private Integer freeAgentUsed;
    private String captainPlayerId;
    private String captainNickname;
    private Boolean currentUserIsCaptain;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date approvedTime;
}
