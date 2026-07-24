package com.zhs.basket.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SettlementValidationResultVo {
    private Long seasonId;
    private boolean passed;
    private int confirmedMatchCount;
    private int unfinishedMatchCount;
    private int reopenedMatchCount;
    private int cancelledMatchCount;
    private int errorCount;
    private boolean standingRuleConfigured;
    private List<SettlementValidationErrorVo> errors = new ArrayList<>();
}
