package com.zhs.basket.vo.v4;

import lombok.Data;

@Data
public class BasketFormatTemplateTieBreakRuleVo {
    private Long id;
    private String stageCode;
    private String ruleCode;
    private String ruleCodeName;
    private Integer priorityNo;
    private Integer enabled;
}
