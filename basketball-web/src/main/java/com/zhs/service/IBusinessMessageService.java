package com.zhs.service;

public interface IBusinessMessageService {

    void sendSiteMessage(Long receiverMemberId, String title, String content,
                         String bizType, Long bizId, String redirectType, String redirectValue);

    void sendSiteMessageFrom(Long senderMemberId, Long receiverMemberId, String title, String content,
                             String bizType, Long bizId, String redirectType, String redirectValue);
}
