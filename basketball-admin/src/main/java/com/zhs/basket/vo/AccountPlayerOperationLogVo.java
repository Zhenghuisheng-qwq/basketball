package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class AccountPlayerOperationLogVo {
    private Long id;
    private String eventType;
    private String eventTitle;
    private String operatorType;
    private Long operatorId;
    private String operatorName;
    private Date eventTime;
    private String remark;
    private String clientIp;
}
