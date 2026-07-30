package com.zhs.basket.req;

import lombok.Data;
import java.util.Date;

@Data
public class ScheduleGenerateReq {
    /** eventId = seasonId */
    private Long eventId;
    private Long ruleId;
    private Long stageId;
    private Date baseDate;
    private Boolean forceRegenerate;
    private String remark;
}
