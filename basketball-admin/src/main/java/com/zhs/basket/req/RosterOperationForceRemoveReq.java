package com.zhs.basket.req;

import lombok.Data;

@Data
public class RosterOperationForceRemoveReq {
    private Long rosterId;
    private String operateReason;
}
