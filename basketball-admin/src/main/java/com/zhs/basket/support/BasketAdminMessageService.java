package com.zhs.basket.support;

import com.zhs.basket.domain.BasketMessageReceiver;
import com.zhs.basket.domain.BasketMessageTask;
import com.zhs.basket.mapper.BasketMessageReceiverMapper;
import com.zhs.basket.mapper.BasketMessageTaskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class BasketAdminMessageService {

    private static final Integer NOT_DELETED = 0;
    private static final String MESSAGE_TYPE_SITE = "SITE";
    private static final String SEND_SCOPE_SINGLE = "SINGLE";
    private static final String SEND_SCOPE_BATCH = "BATCH";
    private static final int SEND_STATUS_SENT = 1;
    private static final String DELIVERY_CHANNEL_SITE = "SITE";
    private static final int DELIVERY_STATUS_SENT = 1;
    private static final int READ_STATUS_UNREAD = 0;

    @Autowired
    private BasketMessageTaskMapper messageTaskMapper;
    @Autowired
    private BasketMessageReceiverMapper messageReceiverMapper;
    @Autowired
    private PlayerNoGenerator playerNoGenerator;

    public void sendSiteMessage(Long memberId, Long adminUserId, String bizType, Long bizId,
                                String title, String content) {
        sendSiteMessage(memberId, adminUserId, bizType, bizId, title, content, null, null);
    }

    public void sendSiteMessage(Long memberId, Long adminUserId, String bizType, Long bizId,
                                String title, String content, String templateCode, String templateParams) {
        if (memberId == null) {
            return;
        }
        sendSiteMessages(java.util.Collections.singletonList(memberId), adminUserId, bizType, bizId,
                title, content, templateCode, templateParams);
    }

    public void sendSiteMessages(List<Long> memberIds, Long adminUserId, String bizType, Long bizId,
                                 String title, String content) {
        sendSiteMessages(memberIds, adminUserId, bizType, bizId, title, content, null, null);
    }

    public void sendSiteMessages(List<Long> memberIds, Long adminUserId, String bizType, Long bizId,
                                 String title, String content, String templateCode, String templateParams) {
        if (CollectionUtils.isEmpty(memberIds)) {
            return;
        }
        List<Long> distinctIds = memberIds.stream().filter(id -> id != null && id > 0).distinct()
                .collect(java.util.stream.Collectors.toList());
        if (distinctIds.isEmpty()) {
            return;
        }
        Date now = new Date();
        BasketMessageTask task = new BasketMessageTask();
        task.setTaskNo(playerNoGenerator.generateBizNo("MT"));
        task.setMessageType(MESSAGE_TYPE_SITE);
        task.setTitle(title);
        task.setContentText(content);
        task.setTemplateCode(templateCode);
        task.setTemplateParams(templateParams);
        task.setBizType(bizType);
        task.setBizId(bizId);
        task.setSendScope(distinctIds.size() == 1 ? SEND_SCOPE_SINGLE : SEND_SCOPE_BATCH);
        task.setSendStatus(SEND_STATUS_SENT);
        task.setSentTime(now);
        task.setTotalCount(distinctIds.size());
        task.setSuccessCount(distinctIds.size());
        task.setFailCount(0);
        task.setCreatorId(adminUserId);
        task.setCreateTime(now);
        task.setUpdateTime(now);
        task.setIsDelete(NOT_DELETED);
        messageTaskMapper.insert(task);

        List<BasketMessageReceiver> receivers = new ArrayList<>(distinctIds.size());
        for (Long memberId : distinctIds) {
            BasketMessageReceiver receiver = new BasketMessageReceiver();
            receiver.setTaskId(task.getId());
            receiver.setMemberId(memberId);
            receiver.setDeliveryChannel(DELIVERY_CHANNEL_SITE);
            receiver.setDeliveryStatus(DELIVERY_STATUS_SENT);
            receiver.setReadStatus(READ_STATUS_UNREAD);
            receiver.setSentTime(now);
            receiver.setCreateTime(now);
            receiver.setUpdateTime(now);
            receiver.setIsDelete(NOT_DELETED);
            receivers.add(receiver);
        }
        messageReceiverMapper.batchInsert(receivers);
    }
}
