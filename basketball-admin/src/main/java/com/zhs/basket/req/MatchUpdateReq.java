package com.zhs.basket.req;

import lombok.Data;
import java.util.Date;

@Data
public class MatchUpdateReq {
    private Long roundId;
    private Long groupId;
    private Long venueId;
    private Long homeTeamId;
    private Long awayTeamId;
    private Date scheduledStartTime;
    private Date scheduledEndTime;
    private Integer sortNo;
    private Integer publicStatus;
}
