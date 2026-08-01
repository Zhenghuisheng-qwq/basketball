package com.zhs.basket.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BatchAuditResultVo {
    private int successCount;
    private int failCount;
    private List<BatchAuditFailureVo> failures = new ArrayList<>();
}
