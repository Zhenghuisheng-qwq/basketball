package com.zhs.basket.vo.v4;

import lombok.Data;

@Data
public class BasketDivisionTieBreakRuleVo {
    private Long id;
    private Long stageId;
    private String ruleCode;
    private String ruleCodeName;
    private Integer priorityNo;
    private Integer enabled;
}
