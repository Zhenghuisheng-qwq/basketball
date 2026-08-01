package com.zhs.basket.req;

import lombok.Data;

@Data
public class SchedulePublishReq {
    private Long eventId;
    private Long batchId;
    private String reason;
    /** 为 true 时允许草稿场次少于预计总场次仍发布 */
    private Boolean allowIncomplete;
}
