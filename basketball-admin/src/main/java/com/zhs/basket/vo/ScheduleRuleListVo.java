package com.zhs.basket.vo;

import lombok.Data;

@Data
public class ScheduleRuleListVo {
    private Long id;
    private String ruleName;
    private String ruleCode;
    private String ruleVersion;
    private String eventType;
    private String platformCode;
    private Integer enabled;
    private String remark;
}
