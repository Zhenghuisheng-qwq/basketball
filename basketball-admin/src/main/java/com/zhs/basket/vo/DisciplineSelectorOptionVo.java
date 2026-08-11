package com.zhs.basket.vo;

import lombok.Data;

@Data
public class DisciplineSelectorOptionVo {
    private Long id;
    private String label;
    private String secondaryLabel;
    private String status;
    private Long memberId;
    private Long teamId;
    private Long eventTeamId;
    private Long eventPlayerId;
    private String teamName;
    private String playerNo;
    private String gameId;
    private String divisionName;
    private String platformCode;
    private String gameMode;
}