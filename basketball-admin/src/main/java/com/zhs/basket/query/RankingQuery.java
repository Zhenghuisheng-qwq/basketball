package com.zhs.basket.query;

import lombok.Data;

@Data
public class RankingQuery {
    private Long seasonId;
    private Long stageId;
    private String rankType;
    private Long teamId;
    private String keyword;
    /** TEAM / PLAYER */
    private String subjectType;
}
