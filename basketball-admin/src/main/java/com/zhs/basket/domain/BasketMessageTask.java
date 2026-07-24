package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketMessageTask {
    private Long id;
    private String taskNo;
    private String messageType;
    private String title;
    private String contentText;
    private String bizType;
    private Long bizId;
    private String sendScope;
    private Integer sendStatus;
    private Date scheduledTime;
    private Date sentTime;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private Long creatorId;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
