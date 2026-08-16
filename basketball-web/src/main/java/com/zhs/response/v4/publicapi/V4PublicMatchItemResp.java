package com.zhs.response.v4.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.Date;

@Data
public class V4PublicMatchItemResp {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long matchId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long divisionId;

    private Integer roundNo;

    private String matchNo;

    private Date matchTime;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long homeEventTeamId;

    private String homeTeamName;

    private String homeSourceType;

    private String homeSourceDisplay;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long homeTeamLogoFileId;

    private String homeTeamLogoUrl;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long awayEventTeamId;

    private String awayTeamName;

    private String awaySourceType;

    private String awaySourceDisplay;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long awayTeamLogoFileId;

    private String awayTeamLogoUrl;

    private Integer homeScore;

    private Integer awayScore;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long winnerEventTeamId;

    private String matchStatus;

    private String publishStatus;

    private String venueText;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long matchSeriesId;

    private Integer seriesGameNo;

    private Integer bestOf;

    private Integer seriesHomeWinCount;

    private Integer seriesAwayWinCount;

    private String seriesStatus;

    private String seriesStatusName;

    private String resultType;

    private String resultReason;

    private Boolean homeDisqualified;

    private Boolean awayDisqualified;

    private String homeResultLabel;

    private String awayResultLabel;
}
