package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.List;

@Data
public class BasketMatchSeriesDetailVo {
    private Long id;
    private String seriesCode;
    private Long seasonId;
    private String seasonName;
    private Long divisionId;
    private String divisionName;
    private Long stageId;
    private String stageName;
    private Integer roundNo;
    private String roundName;
    private Integer bracketSlot;
    private java.time.LocalDate scheduledDate;
    private java.time.LocalTime scheduledStartTime;
    private String publishStatus;
    private String publishStatusName;
    private Integer gamesPlayed;
    private String seriesScore;
    private Long homeEventTeamId;
    private String homeTeamName;
    private Long homeTeamLogoFileId;
    private Long awayEventTeamId;
    private String awayTeamName;
    private Long awayTeamLogoFileId;
    private Integer bestOf;
    private Integer requiredWins;
    private Integer homeWinCount;
    private Integer awayWinCount;
    private String seriesStatus;
    private String seriesStatusName;
    private String resolutionType;
    private String resolutionReason;
    private Long winnerEventTeamId;
    private String winnerTeamName;
    private String qualificationStatus;
    private String qualificationStatusName;
    private Boolean finalStage;
    private String resultLabel;
    private boolean canAdjustSchedule;
    private String adjustScheduleBlockReason;
    private boolean canResolveSeriesManually;
    private boolean canForfeitSeries;
    private List<BasketMatchSeriesGameVo> games;
}
