package com.zhs.basket.vo;

import lombok.Data;

@Data
public class MatchPeriodScoreVo {
    private Long id;
    private Integer periodNo;
    private String periodType;
    private String periodTypeName;
    private Integer homeScore;
    private Integer awayScore;
}
