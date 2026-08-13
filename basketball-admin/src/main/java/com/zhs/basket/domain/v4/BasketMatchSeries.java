package com.zhs.basket.domain.v4;

import lombok.Data;

import java.util.Date;

/**
 * CNPA 赛事域 V4 实体：BasketMatchSeries（BO3/BO5 系列赛）
 */
@Data
public class BasketMatchSeries {
    private Long id;
    private Long divisionId;
    private Long stageId;
    private Long scheduleBatchId;
    private String seriesCode;
    private Integer roundNo;
    private Integer bracketSlot;
    private Long homeEventTeamId;
    private Long awayEventTeamId;
    private String homeSourceType;
    private String awaySourceType;
    private String homeSourceDisplay;
    private String awaySourceDisplay;
    private Integer bestOf;
    private Integer homeWinCount;
    private Integer awayWinCount;
    private Long winnerEventTeamId;
    private String seriesStatus;
    private String resolutionType;
    private String resolutionReason;
    private Date resolvedTime;
    private Long resolvedBy;
    private Long sourceSeriesIdHome;
    private Long sourceSeriesIdAway;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
