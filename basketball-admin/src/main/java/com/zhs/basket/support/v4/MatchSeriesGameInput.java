package com.zhs.basket.support.v4;

import lombok.Data;

/**
 * 系列赛单局结果输入（计算器用）。
 */
@Data
public class MatchSeriesGameInput {
    private Long matchId;
    private Long seriesId;
    private Integer seriesGameNo;
    private Long homeEventTeamId;
    private Long awayEventTeamId;
    private Long winnerEventTeamId;
    private String matchStatus;
}
