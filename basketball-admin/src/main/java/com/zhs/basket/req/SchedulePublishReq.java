package com.zhs.basket.req;

import lombok.Data;

@Data
public class SchedulePublishReq {
    private Long eventId;
    private Long batchId;
    private String reason;
}
