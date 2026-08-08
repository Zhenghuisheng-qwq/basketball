package com.zhs.basket.support.v4;

import lombok.Data;

/**
 * 系列赛聚合结果（计算器输出）。
 */
@Data
public class MatchSeriesResultSnapshot {
    private int requiredWins;
    private int homeWinCount;
    private int awayWinCount;
    private Long winnerEventTeamId;
    private boolean completed;
}
