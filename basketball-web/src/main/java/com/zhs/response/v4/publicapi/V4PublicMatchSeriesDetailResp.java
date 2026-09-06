package com.zhs.response.v4.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.List;

@Data
public class V4PublicMatchSeriesDetailResp {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String seriesCode;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long divisionId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long stageId;
    private String stageName;
    private Integer roundNo;
    private String roundName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long homeEventTeamId;
    private String homeTeamName;
    private String homeTeamShortName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long homeTeamLogoFileId;
    private String homeTeamLogoUrl;
    private String homeSourceType;
    private String homeSourceDisplay;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long awayEventTeamId;
    private String awayTeamName;
    private String awayTeamShortName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long awayTeamLogoFileId;
    private String awayTeamLogoUrl;
    private String awaySourceType;
    private String awaySourceDisplay;
    private java.time.LocalDate scheduledDate;
    private java.time.LocalTime scheduledStartTime;
    private Integer bestOf;
    private Integer requiredWins;
    private Integer homeWinCount;
    private Integer awayWinCount;
    private String seriesStatus;
    private String seriesStatusName;
    private String resolutionType;
    private String resolutionTypeName;
    private String resultSummary;
    private String resultReason;
    private Boolean homeDisqualified;
    private Boolean awayDisqualified;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long winnerEventTeamId;
    private String winnerTeamName;
    private Boolean finalStage;
    private String resultLabel;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;
    private String seasonName;
    private String eventSeriesCode;
    private String eventSeriesName;
    private String divisionName;
    private String gameMode;
    private List<V4PublicMatchSeriesGameResp> games;
    private List<V4PublicMatchTeamStatItemResp> teamStats;
    private List<V4PublicMatchPlayerStatItemResp> playerStats;
}
