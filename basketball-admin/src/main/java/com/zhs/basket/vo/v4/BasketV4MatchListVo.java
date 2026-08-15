package com.zhs.basket.vo.v4;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class BasketV4MatchListVo {
    private Long id;
    private Long seasonId;
    private String seasonTitle;
    private Long divisionId;
    private String divisionName;
    private String platformCode;
    private Long scheduleBatchId;
    private Integer roundNo;
    private String matchNo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date matchTime;
    private Long homeEventTeamId;
    private String homeTeamName;
    private Long awayEventTeamId;
    private String awayTeamName;
    private Integer homeScore;
    private Integer awayScore;
    private Long winnerEventTeamId;
    private String matchStatus;
    private String publishStatus;
    private String venueText;
    private String remark;
    private Long stageId;
    private Long seriesId;
    private Integer seriesGameNo;
    private String seriesCode;
    private Integer bestOf;
    private boolean standaloneMatch;
    private boolean canAdjustSchedule;
    private String adjustScheduleBlockReason;
    private boolean canEnterOfficialResult;
    private boolean canCorrectOfficialResult;
    private boolean canForfeitMatch;
}
