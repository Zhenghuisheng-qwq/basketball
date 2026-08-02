package com.zhs.basket.vo;

import lombok.Data;

import java.util.List;

@Data
public class MatchDataSubmissionComparisonSummaryVo {
    private Boolean opponentSubmitted;
    private String summaryText;
    private Integer diffCount;
    private List<MatchDataSubmissionComparisonDiffVo> diffs;
}
