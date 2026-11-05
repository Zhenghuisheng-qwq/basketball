package com.zhs.basket.resp;

import lombok.Data;

import java.util.Date;

@Data
public class MailNoticeResp {
    private Long id;
    private String title;
    private String contentHtml;
    private Integer sendStatus;
    private String sendStatusName;
    private Integer recipientCount;
    private Integer successCount;
    private Integer failCount;
    private String createBy;
    private Date createTime;
    private Date updateTime;
    private Date sendTime;
}
