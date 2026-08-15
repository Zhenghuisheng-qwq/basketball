package com.zhs.basket.dto.v4;

import lombok.Data;

@Data
public class ScheduleRescheduleReq {
    private String unitType;
    private Long unitId;
    private String scheduledDate;
    private String scheduledStartTime;
    private String reason;
}
