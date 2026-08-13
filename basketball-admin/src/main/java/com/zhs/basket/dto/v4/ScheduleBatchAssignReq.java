package com.zhs.basket.dto.v4;

import lombok.Data;

import java.util.List;

@Data
public class ScheduleBatchAssignReq {
    private String scheduledDate;
    private String scheduledStartTime;
    private Integer roundNo;
    private List<ScheduleAssignReq> items;
    private String scheduleSource;
}
