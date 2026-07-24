package com.zhs.basket.req;

import lombok.Data;
import java.util.Date;

@Data
public class MatchScheduleReq {
    private Long venueId;
    private Date scheduledStartTime;
    private Date scheduledEndTime;
}
