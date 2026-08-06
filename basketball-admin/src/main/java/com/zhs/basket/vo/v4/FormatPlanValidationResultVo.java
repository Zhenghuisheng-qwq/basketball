package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 赛制方案统一校验结果。
 */
@Data
public class FormatPlanValidationResultVo {
    private boolean valid;
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private Integer qualifierCount;
    private Integer expectedGroupSize;
    private Integer expectedGroupMatchCount;
    private Integer expectedEliminationMatchCount;
    private Integer expectedTotalMatchCount;
    private String eliminationStartRound;
}
