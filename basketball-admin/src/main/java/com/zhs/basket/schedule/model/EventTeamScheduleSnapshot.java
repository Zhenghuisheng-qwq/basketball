package com.zhs.basket.schedule.model;

import lombok.Data;
import java.util.Date;

@Data
public class EventTeamScheduleSnapshot {
    private Long eventTeamId;
    private Long teamId;
    private String teamName;
    private String logoUrl;
}
