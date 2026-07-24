package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketMessageReceiverDao;
import com.zhs.dao.BasketMessageTaskDao;
import com.zhs.enums.MessageReadStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketMessageReceiver;
import com.zhs.model.BasketMessageTask;
import com.zhs.response.account.MessageItemResp;
import com.zhs.service.IAccountMessageService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AccountMessageServiceImpl implements IAccountMessageService {

    private static final Integer NOT_DELETED = 0;

    @Resource
    private BasketMessageReceiverDao messageReceiverDao;
    @Resource
    private BasketMessageTaskDao messageTaskDao;

    @Override
    public IPage<MessageItemResp> pageMessages(Integer currentPage, Integer pageSize, Integer readStatus) {
        Long memberId = UserContext.getRequiredUserId();
        Page<BasketMessageReceiver> page = new Page<>(normalizePage(currentPage), normalizeSize(pageSize));

        LambdaQueryWrapper<BasketMessageReceiver> wrapper = baseReceiverWrapper(memberId);
        if (readStatus != null) {
            wrapper.eq(BasketMessageReceiver::getReadStatus, readStatus);
        }
        wrapper.orderByDesc(BasketMessageReceiver::getSentTime)
                .orderByDesc(BasketMessageReceiver::getId);

        IPage<BasketMessageReceiver> receiverPage = messageReceiverDao.selectPage(page, wrapper);
        Map<Long, BasketMessageTask> taskMap = loadTaskMap(receiverPage.getRecords());

        Page<MessageItemResp> respPage = new Page<>(receiverPage.getCurrent(), receiverPage.getSize(), receiverPage.getTotal());
        respPage.setRecords(buildMessageItems(receiverPage.getRecords(), taskMap));
        return respPage;
    }

    @Override
    public long countUnread() {
        Long memberId = UserContext.getRequiredUserId();
        return messageReceiverDao.selectCount(baseReceiverWrapper(memberId)
                .eq(BasketMessageReceiver::getReadStatus, MessageReadStatusEnum.UNREAD.getCode()));
    }

    @Override
    public MessageItemResp getMessage(Long receiverId) {
        if (receiverId == null) {
            throw new ServiceException("消息主键不能为空");
        }
        Long memberId = UserContext.getRequiredUserId();
        BasketMessageReceiver receiver = requireOwnedReceiver(receiverId, memberId);
        Map<Long, BasketMessageTask> taskMap = loadTaskMap(Collections.singletonList(receiver));
        return buildMessageItem(receiver, taskMap.get(receiver.getTaskId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long receiverId) {
        if (receiverId == null) {
            throw new ServiceException("消息主键不能为空");
        }
        Long memberId = UserContext.getRequiredUserId();
        Date now = new Date();

        LambdaUpdateWrapper<BasketMessageReceiver> updateWrapper = new LambdaUpdateWrapper<BasketMessageReceiver>()
                .eq(BasketMessageReceiver::getId, receiverId)
                .eq(BasketMessageReceiver::getMemberId, memberId)
                .eq(BasketMessageReceiver::getIsDelete, NOT_DELETED)
                .eq(BasketMessageReceiver::getReadStatus, MessageReadStatusEnum.UNREAD.getCode())
                .set(BasketMessageReceiver::getReadStatus, MessageReadStatusEnum.READ.getCode())
                .set(BasketMessageReceiver::getReadTime, now);

        int updated = messageReceiverDao.update(null, updateWrapper);
        if (updated > 0) {
            return;
        }

        BasketMessageReceiver existing = requireOwnedReceiver(receiverId, memberId);
        if (!Objects.equals(existing.getReadStatus(), MessageReadStatusEnum.READ.getCode())) {
            throw new ServiceException("标记已读失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead() {
        Long memberId = UserContext.getRequiredUserId();
        Date now = new Date();
        messageReceiverDao.update(null, new LambdaUpdateWrapper<BasketMessageReceiver>()
                .eq(BasketMessageReceiver::getMemberId, memberId)
                .eq(BasketMessageReceiver::getIsDelete, NOT_DELETED)
                .eq(BasketMessageReceiver::getReadStatus, MessageReadStatusEnum.UNREAD.getCode())
                .set(BasketMessageReceiver::getReadStatus, MessageReadStatusEnum.READ.getCode())
                .set(BasketMessageReceiver::getReadTime, now));
    }

    @Override
    public List<MessageItemResp> listRecentMessages(int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        Long memberId = UserContext.getRequiredUserId();
        List<BasketMessageReceiver> receivers = messageReceiverDao.selectList(baseReceiverWrapper(memberId)
                .orderByDesc(BasketMessageReceiver::getSentTime)
                .orderByDesc(BasketMessageReceiver::getId)
                .last("limit " + limit));
        if (CollectionUtils.isEmpty(receivers)) {
            return Collections.emptyList();
        }
        Map<Long, BasketMessageTask> taskMap = loadTaskMap(receivers);
        return buildMessageItems(receivers, taskMap);
    }

    private LambdaQueryWrapper<BasketMessageReceiver> baseReceiverWrapper(Long memberId) {
        return new LambdaQueryWrapper<BasketMessageReceiver>()
                .eq(BasketMessageReceiver::getMemberId, memberId)
                .eq(BasketMessageReceiver::getIsDelete, NOT_DELETED);
    }

    private BasketMessageReceiver requireOwnedReceiver(Long receiverId, Long memberId) {
        BasketMessageReceiver receiver = messageReceiverDao.selectOne(new LambdaQueryWrapper<BasketMessageReceiver>()
                .eq(BasketMessageReceiver::getId, receiverId)
                .eq(BasketMessageReceiver::getMemberId, memberId)
                .eq(BasketMessageReceiver::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        if (receiver == null) {
            throw new ServiceException("消息不存在");
        }
        return receiver;
    }

    private Map<Long, BasketMessageTask> loadTaskMap(List<BasketMessageReceiver> receivers) {
        if (CollectionUtils.isEmpty(receivers)) {
            return Collections.emptyMap();
        }
        Set<Long> taskIds = receivers.stream()
                .map(BasketMessageReceiver::getTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BasketMessageTask> tasks = messageTaskDao.selectBatchIds(taskIds);
        if (CollectionUtils.isEmpty(tasks)) {
            return Collections.emptyMap();
        }
        Map<Long, BasketMessageTask> taskMap = new HashMap<>(tasks.size());
        for (BasketMessageTask task : tasks) {
            if (task != null && NOT_DELETED.equals(task.getIsDelete())) {
                taskMap.put(task.getId(), task);
            }
        }
        return taskMap;
    }

    private List<MessageItemResp> buildMessageItems(List<BasketMessageReceiver> receivers,
                                                    Map<Long, BasketMessageTask> taskMap) {
        if (CollectionUtils.isEmpty(receivers)) {
            return Collections.emptyList();
        }
        List<MessageItemResp> items = new ArrayList<>(receivers.size());
        for (BasketMessageReceiver receiver : receivers) {
            items.add(buildMessageItem(receiver, taskMap.get(receiver.getTaskId())));
        }
        return items;
    }

    private MessageItemResp buildMessageItem(BasketMessageReceiver receiver, BasketMessageTask task) {
        MessageItemResp resp = new MessageItemResp();
        resp.setReceiverId(String.valueOf(receiver.getId()));
        resp.setReadStatus(receiver.getReadStatus());
        resp.setSentTime(receiver.getSentTime());
        resp.setReadTime(receiver.getReadTime());
        resp.setRedirectType(receiver.getRedirectType());
        resp.setRedirectValue(receiver.getRedirectValue());
        if (task != null) {
            resp.setMessageType(task.getMessageType());
            resp.setTitle(task.getTitle());
            resp.setContent(toContentString(task.getContentText()));
            resp.setBizType(task.getBizType());
            resp.setBizId(task.getBizId() == null ? null : String.valueOf(task.getBizId()));
        }
        return resp;
    }

    private String toContentString(Object contentText) {
        if (contentText == null) {
            return null;
        }
        if (contentText instanceof String text) {
            return text;
        }
        return contentText.toString();
    }

    private long normalizePage(Integer currentPage) {
        return currentPage == null || currentPage < 1 ? 1L : currentPage;
    }

    private long normalizeSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10L : pageSize;
    }
}
