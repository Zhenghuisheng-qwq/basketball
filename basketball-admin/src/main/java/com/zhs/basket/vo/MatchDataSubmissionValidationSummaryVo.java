package com.zhs.basket.vo;

import lombok.Data;

import java.util.List;

@Data
public class MatchDataSubmissionValidationSummaryVo {
    private Boolean allPassed;
    private Integer errorCount;
    private Integer warningCount;
    private List<MatchDataSubmissionValidationItemVo> items;
}
