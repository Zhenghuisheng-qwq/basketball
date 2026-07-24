package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketMessageReceiver {
    private Long id;
    private Long taskId;
    private Long memberId;
    private String deliveryChannel;
    private Integer deliveryStatus;
    private Integer readStatus;
    private Date sentTime;
    private Date readTime;
    private String failureReason;
    private String redirectType;
    private String redirectValue;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
