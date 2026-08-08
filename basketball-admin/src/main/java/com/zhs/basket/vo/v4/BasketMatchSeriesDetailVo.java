package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.List;

@Data
public class BasketMatchSeriesDetailVo {
    private Long id;
    private String seriesCode;
    private Long divisionId;
    private Long stageId;
    private String stageName;
    private Integer roundNo;
    private Integer bracketSlot;
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
    private List<BasketMatchSeriesGameVo> games;
}
