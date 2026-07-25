package com.zhs.basket.query;

import lombok.Data;

@Data
public class DisciplineQuery {
    private Long seasonId;
    private Long tournamentId;
    private Long teamId;
    private Long playerId;
    private Long memberId;
    private String targetType;
    private String recordStatus;
    private String penaltyCode;
    private String keyword;
}
