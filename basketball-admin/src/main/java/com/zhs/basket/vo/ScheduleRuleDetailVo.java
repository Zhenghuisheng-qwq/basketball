package com.zhs.basket.vo;

import lombok.Data;
import java.util.List;

@Data
public class ScheduleRuleDetailVo {
    private Long id;
    private String ruleName;
    private String ruleCode;
    private String ruleVersion;
    private String eventType;
    private String platformCode;
    private Integer enabled;
    private String remark;
    private List<ScheduleRuleWeekdayVo> weekdays;
    private List<ScheduleRuleTimeSlotVo> timeSlots;
}
