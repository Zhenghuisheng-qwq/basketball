package com.zhs.basket.req;

import lombok.Data;

@Data
public class MailNoticeQueryReq {
    private Integer sendStatus;
    private String keyword;
}
