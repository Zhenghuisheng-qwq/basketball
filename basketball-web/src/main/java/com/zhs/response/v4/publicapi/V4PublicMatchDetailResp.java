package com.zhs.response.v4.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class V4PublicMatchDetailResp {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long matchId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seriesId;

    private String seriesName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;

    private String seasonName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long divisionId;

    private String divisionName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long stageId;

    private String stageName;

    private String stageType;

    private Integer roundNo;

    private String matchNo;

    private Date matchTime;

    private String matchStatus;

    private String matchStatusName;

    private String publishStatus;

    private String venueText;

    private Integer homeScore;

    private Integer awayScore;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long winnerEventTeamId;

    private V4PublicEventTeamDetailResp homeTeam;

    private V4PublicEventTeamDetailResp awayTeam;

    private List<V4PublicMatchTeamStatItemResp> teamStats;

    private List<V4PublicMatchPlayerStatItemResp> playerStats;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long matchSeriesId;

    private Integer seriesGameNo;

    private Integer bestOf;

    private V4PublicMatchSeriesDetailResp seriesContext;
}
