package com.zhs.basket.vo;

import lombok.Data;
import java.util.Date;

@Data
public class StatChangeRecordVo {
    private Long id;
    private String bizType;
    private Long bizId;
    private String fieldName;
    private String beforeValue;
    private String afterValue;
    private String changeReason;
    private Long operatorId;
    private String operatorName;
    private Date createTime;
}
