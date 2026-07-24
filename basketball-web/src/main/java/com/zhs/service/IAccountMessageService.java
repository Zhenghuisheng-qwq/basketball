package com.zhs.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.response.account.MessageItemResp;

import java.util.List;

/**
 * 账户消息业务服务
 */
public interface IAccountMessageService {

    IPage<MessageItemResp> pageMessages(Integer currentPage, Integer pageSize, Integer readStatus);

    long countUnread();

    MessageItemResp getMessage(Long receiverId);

    void markRead(Long receiverId);

    void markAllRead();

    List<MessageItemResp> listRecentMessages(int limit);
}
