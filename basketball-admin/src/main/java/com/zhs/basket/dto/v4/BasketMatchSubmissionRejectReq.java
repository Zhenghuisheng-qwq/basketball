package com.zhs.basket.dto.v4;

import lombok.Data;

@Data
public class BasketMatchSubmissionRejectReq {
    private String rejectReason;
    private String auditRemark;
}
