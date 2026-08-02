package com.zhs.basket.vo;

import lombok.Data;

@Data
public class MatchDataSubmissionPeriodVo {
    private Integer periodNo;
    private String periodType;
    private String periodTypeName;
    private Integer homeScore;
    private Integer awayScore;
    private Integer scoreDiff;
    private String validationStatus;
    private Integer sortNo;
}
