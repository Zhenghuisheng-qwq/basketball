package com.zhs.basket.req;

import lombok.Data;

@Data
public class AdminMatchDataSubmissionRejectReq {
    private String rejectReason;
    private String auditRemark;
}
