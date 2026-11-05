package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketMailNotice {
    private Long id;
    private String title;
    private String contentHtml;
    private Integer sendStatus;
    private Integer recipientCount;
    private Integer successCount;
    private Integer failCount;
    private String createBy;
    private Date createTime;
    private Date updateTime;
    private Date sendTime;
    private Integer isDelete;
}
