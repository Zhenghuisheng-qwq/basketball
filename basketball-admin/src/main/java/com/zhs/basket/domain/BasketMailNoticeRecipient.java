package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketMailNoticeRecipient {
    private Long id;
    private Long noticeId;
    private Long memberId;
    private String username;
    private String nickname;
    private String email;
    private Integer sendStatus;
    private String failReason;
    private Date sendTime;
}
