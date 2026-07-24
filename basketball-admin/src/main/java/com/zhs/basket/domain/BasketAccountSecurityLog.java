package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketAccountSecurityLog {
    private Long id;
    private Long memberId;
    private String eventType;
    private String eventTitle;
    private String oldValueMasked;
    private String newValueMasked;
    private String operatorType;
    private Long operatorId;
    private String clientIp;
    private String terminalType;
    private Date eventTime;
    private String remark;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
