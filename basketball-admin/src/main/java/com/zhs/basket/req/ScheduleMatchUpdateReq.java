package com.zhs.basket.req;

import lombok.Data;
import java.util.Date;

@Data
public class ScheduleMatchUpdateReq {
    private Long stageId;
    private Integer roundNo;
    private Long homeEventTeamId;
    private Long awayEventTeamId;
    private Date matchTime;
    private Integer matchStatus;
    private Integer publishStatus;
    private String remark;
    private String reason;
}
