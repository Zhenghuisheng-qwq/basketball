package com.zhs.basket.req;

import lombok.Data;
import java.util.Date;

@Data
public class MatchPostponeReq {
    private String reason;
    private Date scheduledStartTime;
    private Date scheduledEndTime;
    private Long venueId;
}
