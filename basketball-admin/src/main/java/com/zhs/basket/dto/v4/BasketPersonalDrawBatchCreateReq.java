package com.zhs.basket.dto.v4;

import lombok.Data;

@Data
public class BasketPersonalDrawBatchCreateReq {
    private Long divisionId;
    private String batchCode;
    private String batchName;
    private String resultSource;
    private String resultRemark;
}
