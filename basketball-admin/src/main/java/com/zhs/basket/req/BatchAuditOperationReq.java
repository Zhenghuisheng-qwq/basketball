package com.zhs.basket.req;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BatchAuditOperationReq {
    @NotEmpty(message = "请选择至少一条记录")
    private List<Long> ids;
    private String auditRemark;
    private String rejectReason;
    private String positionCode;
}