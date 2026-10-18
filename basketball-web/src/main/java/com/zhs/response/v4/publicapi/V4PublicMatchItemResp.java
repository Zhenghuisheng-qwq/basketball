package com.zhs.response.v4.publicapi;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    @JsonSerialize(using = ToStringSerializer.class)
    private Long stageId;

    private String stageName;

    private String stageType;

    private String competitionFormat;

    private Integer roundNo;

    private String matchNo;

    /** 子局或 BO1 的独立开赛时刻。无独立排期时为空，不借用系列赛时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Shanghai")
    private Date matchTime;

    /** 系列赛排期日期，Asia/Shanghai 墙钟 yyyy-MM-dd。 */
    private String seriesScheduleDate;

    /** 系列赛开赛钟面，Asia/Shanghai HH:mm。 */
    private String seriesStartTime;

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

    private Integer originalHomeScore;

    private Integer originalAwayScore;

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
