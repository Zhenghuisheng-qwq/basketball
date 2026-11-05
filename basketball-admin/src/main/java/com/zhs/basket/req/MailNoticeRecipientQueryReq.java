package com.zhs.basket.req;

import lombok.Data;

@Data
public class MailNoticeRecipientQueryReq {
    private Long noticeId;
    private Integer sendStatus;
    private String keyword;
}
