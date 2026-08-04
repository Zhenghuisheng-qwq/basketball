package com.zhs.basket.vo.v4;

import lombok.Data;

@Data
public class BasketStandingRankReasonVo {
    private String ruleCode;
    private String ruleName;
    private Integer priorityNo;
    private String compareValue;
    private String reasonText;
    private Integer isDecisive;
}
