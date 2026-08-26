package com.zhs.response.v4.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.Date;

@Data
public class V4PublicMatchSeriesGameResp {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long matchId;
    private String matchCode;
    private Integer seriesGameNo;
    private String matchStatus;
    private String matchStatusName;
    private Integer homeScore;
    private Integer awayScore;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long winnerEventTeamId;
    private Date scheduledTime;
    private Boolean skipped;
    private String skippedReason;
    private Boolean currentGame;

    private String replayUrl;
}
