package com.zhs.response.v4.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class V4PublicScheduleItemResp {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String unitType;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long stageId;

    private String stageName;
    private Integer roundNo;
    private String roundName;
    private String seriesCode;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long homeEventTeamId;

    private String homeTeam;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long awayEventTeamId;

    private String awayTeam;
    private Integer bestOf;
    private String seriesScore;
    private String gameProgress;
    private String scheduleDate;
    private String startTime;
    private String status;
    private String statusName;
    private String placeholderSource;
    private Boolean published;
    private Integer homeWinCount;
    private Integer awayWinCount;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long winnerEventTeamId;
    private Integer requiredWins;
    private Integer gamesPlayed;

    private String resultType;

    private String resultReason;

    private Boolean homeDisqualified;

    private Boolean awayDisqualified;
}
