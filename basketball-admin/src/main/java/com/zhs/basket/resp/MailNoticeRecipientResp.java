package com.zhs.basket.resp;

import lombok.Data;

import java.util.Date;

@Data
public class MailNoticeRecipientResp {
    private Long id;
    private Long noticeId;
    private Long memberId;
    private String username;
    private String nickname;
    private String email;
    private Integer sendStatus;
    private String sendStatusName;
    private String failReason;
    private Date sendTime;
}
