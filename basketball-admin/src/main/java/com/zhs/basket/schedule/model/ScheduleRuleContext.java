package com.zhs.basket.schedule.model;

import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class ScheduleRuleContext {
    private Long eventId;
    private Long seasonId;
    private Long stageId;
    private String platformCode;
    private Long ruleId;
    private String ruleCode;
    private String ruleVersion;
    private Date baseDate;
    private List<EventTeamScheduleSnapshot> eventTeams;
    private List<Integer> weekdays;
    private List<String> timeSlots;
    /** 每个时间段最大比赛场次 */
    private Integer slotCapacity;
    /** FILL_FIRST / EVEN_SPLIT */
    private String allocationMode;
}
