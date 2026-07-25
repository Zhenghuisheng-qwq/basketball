package com.zhs.service.impl;

import cn.hutool.core.util.IdUtil;
import com.zhs.dao.BasketMessageReceiverDao;
import com.zhs.dao.BasketMessageTaskDao;
import com.zhs.enums.MessageReadStatusEnum;
import com.zhs.model.BasketMessageReceiver;
import com.zhs.model.BasketMessageTask;
import com.zhs.service.IBusinessMessageService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class BusinessMessageServiceImpl implements IBusinessMessageService {

    private static final Integer NOT_DELETED = 0;
    private static final int SEND_STATUS_SENT = 1;
    private static final int DELIVERY_STATUS_SENT = 1;
    private static final String MESSAGE_TYPE_SITE = "SITE";
    private static final String SEND_SCOPE_SINGLE = "SINGLE";
    private static final String DELIVERY_CHANNEL_SITE = "SITE";

    @Resource
    private BasketMessageTaskDao messageTaskDao;
    @Resource
    private BasketMessageReceiverDao messageReceiverDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendSiteMessage(Long receiverMemberId, String title, String content,
                                String bizType, Long bizId, String redirectType, String redirectValue) {
        sendSiteMessageFrom(receiverMemberId, receiverMemberId, title, content, bizType, bizId, redirectType, redirectValue);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendSiteMessageFrom(Long senderMemberId, Long receiverMemberId, String title, String content,
                                    String bizType, Long bizId, String redirectType, String redirectValue) {
        if (receiverMemberId == null) {
            return;
        }
        Date now = new Date();
        BasketMessageTask task = new BasketMessageTask();
        task.setTaskNo("MT" + IdUtil.getSnowflakeNextIdStr());
        task.setMessageType(MESSAGE_TYPE_SITE);
        task.setTitle(title);
        task.setContentText(content);
        task.setBizType(bizType);
        task.setBizId(bizId);
        task.setSendScope(SEND_SCOPE_SINGLE);
        task.setSendStatus(SEND_STATUS_SENT);
        task.setSentTime(now);
        task.setTotalCount(1);
        task.setSuccessCount(1);
        task.setFailCount(0);
        task.setCreatorId(senderMemberId);
        task.setIsDelete(NOT_DELETED);
        messageTaskDao.insert(task);

        BasketMessageReceiver receiver = new BasketMessageReceiver();
        receiver.setTaskId(task.getId());
        receiver.setMemberId(receiverMemberId);
        receiver.setDeliveryChannel(DELIVERY_CHANNEL_SITE);
        receiver.setDeliveryStatus(DELIVERY_STATUS_SENT);
        receiver.setReadStatus(MessageReadStatusEnum.UNREAD.getCode());
        receiver.setSentTime(now);
        receiver.setRedirectType(redirectType);
        receiver.setRedirectValue(redirectValue);
        receiver.setIsDelete(NOT_DELETED);
        messageReceiverDao.insert(receiver);
    }
}
