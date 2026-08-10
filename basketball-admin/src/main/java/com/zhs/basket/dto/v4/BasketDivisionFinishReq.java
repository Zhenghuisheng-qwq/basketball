package com.zhs.basket.dto.v4;

import lombok.Data;

@Data
public class BasketDivisionFinishReq {
    /** 结束原因（可选，写入操作日志/Grant 日志） */
    private String reason;
}
