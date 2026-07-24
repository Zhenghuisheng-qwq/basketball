package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketStatChangeRecord {
    private Long id;
    private String bizType;
    private Long bizId;
    private String fieldName;
    private String beforeValue;
    private String afterValue;
    private String changeReason;
    private Long operatorId;
    private String operatorName;
    private Integer approvalStatus;
    private Long approvedBy;
    private Date approvedTime;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
