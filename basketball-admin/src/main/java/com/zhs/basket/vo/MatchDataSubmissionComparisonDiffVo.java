package com.zhs.basket.vo;

import lombok.Data;

@Data
public class MatchDataSubmissionComparisonDiffVo {
    private String field;
    private String fieldLabel;
    private String selfValue;
    private String opponentValue;
}
