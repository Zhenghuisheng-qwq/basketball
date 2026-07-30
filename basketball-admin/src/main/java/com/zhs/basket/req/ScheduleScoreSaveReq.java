package com.zhs.basket.req;

import lombok.Data;

@Data
public class ScheduleScoreSaveReq {
    private Integer homeScore;
    private Integer awayScore;
    private String reason;
}
