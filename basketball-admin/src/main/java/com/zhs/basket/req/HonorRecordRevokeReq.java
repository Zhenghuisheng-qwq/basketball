package com.zhs.basket.req;

import lombok.Data;

@Data
public class HonorRecordRevokeReq {
    private String reason;
    private String revokeReason;
}
