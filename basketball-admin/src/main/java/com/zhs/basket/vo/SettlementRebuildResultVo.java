package com.zhs.basket.vo;

import lombok.Data;

@Data
public class SettlementRebuildResultVo {
    private Long seasonId;
    private Long stageId;
    private Long groupId;
    private Long matchId;
    private String triggerType;
    private int matchCount;
    private int standingCount;
    private int teamStatCount;
    private int playerStatCount;
    private int teamRankCount;
    private int playerRankCount;
    private boolean success;
    private String message;
}
