package com.zhs.basket.query.v4;

import lombok.Data;

@Data
public class BasketV4MatchQuery {
    private Long id;
    private Long seasonId;
    private Long divisionId;
    private Long scheduleBatchId;
    private Integer roundNo;
    private String matchStatus;
    private String publishStatus;
    private Long eventTeamId;
    private String teamName;
    private Long stageId;
    private Long seriesId;
}
