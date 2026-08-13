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
    @JsonSerialize(using = ToStringSerializer.class)
    private Long homeEventTeamId;
    private String homeTeamName;
    private String homeSourceType;
    private String homeSourceDisplay;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long awayEventTeamId;
    private String awayTeamName;
    private String awaySourceType;
    private String awaySourceDisplay;
    private Integer bestOf;
    private Integer requiredWins;
    private Integer homeWinCount;
    private Integer awayWinCount;
    private String seriesStatus;
    private String seriesStatusName;
    private String resolutionType;
    private String resolutionTypeName;
    private String resultSummary;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long winnerEventTeamId;
    private String winnerTeamName;
    private Boolean finalStage;
    private String resultLabel;
    private List<V4PublicMatchSeriesGameResp> games;
}
