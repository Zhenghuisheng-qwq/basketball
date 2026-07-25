package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketStatSyncRecord {
    private Long id;
    private Long matchId;
    private Long officialResultId;
    private String syncType;
    private String syncStatus;
    private String idempotencyKey;
    private String errorMessage;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
