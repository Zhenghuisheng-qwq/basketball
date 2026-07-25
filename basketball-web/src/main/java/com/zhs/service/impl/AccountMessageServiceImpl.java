package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketMessageReceiverDao;
import com.zhs.dao.BasketMessageTaskDao;
import com.zhs.dao.BasketFileAssetDao;
import com.zhs.dao.BasketMemberProfileDao;
import com.zhs.dao.BasketTeamDao;
import com.zhs.dao.BasketTeamInvitationDao;
import com.zhs.enums.MessageReadStatusEnum;
import com.zhs.enums.TeamInvitationStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketFileAsset;
import com.zhs.model.BasketMemberProfile;
import com.zhs.model.BasketMessageReceiver;
import com.zhs.model.BasketMessageTask;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamInvitation;
import com.zhs.response.account.MessageItemResp;
import com.zhs.response.account.MessageTeamInviteInfoResp;
import com.zhs.service.IAccountMessageService;
import com.zhs.service.ICaptainPermissionService;
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
    private static final String BIZ_TYPE_TEAM_INVITE = "TEAM_INVITE";

    @Resource
    private BasketMessageReceiverDao messageReceiverDao;
    @Resource
    private BasketMessageTaskDao messageTaskDao;
    @Resource
    private BasketTeamInvitationDao teamInvitationDao;
    @Resource
    private BasketTeamDao teamDao;
    @Resource
    private BasketMemberProfileDao memberProfileDao;
    @Resource
    private BasketFileAssetDao fileAssetDao;
    @Resource
    private ICaptainPermissionService captainPermissionService;

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
        return buildMessageItem(receiver, taskMap.get(receiver.getTaskId()), memberId, true);
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
            items.add(buildMessageItem(receiver, taskMap.get(receiver.getTaskId()), null, false));
        }
        return items;
    }

    private MessageItemResp buildMessageItem(BasketMessageReceiver receiver, BasketMessageTask task,
                                               Long memberId, boolean includeTeamInvite) {
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
            if (includeTeamInvite && memberId != null) {
                resp.setTeamInvite(loadTeamInviteInfo(task, memberId));
            }
        }
        return resp;
    }

    private MessageTeamInviteInfoResp loadTeamInviteInfo(BasketMessageTask task, Long memberId) {
        if (task == null || !BIZ_TYPE_TEAM_INVITE.equals(task.getBizType()) || task.getBizId() == null) {
            return null;
        }
        BasketTeamInvitation invitation = teamInvitationDao.selectById(task.getBizId());
        if (invitation == null || !NOT_DELETED.equals(invitation.getIsDelete())) {
            return null;
        }
        if (!memberId.equals(invitation.getInviteeMemberId())) {
            return null;
        }

        Date now = new Date();
        Integer displayStatus = invitation.getInvitationStatus();
        if (Objects.equals(TeamInvitationStatusEnum.PENDING.getCode(), displayStatus)
                && invitation.getExpireTime() != null
                && invitation.getExpireTime().before(now)) {
            displayStatus = TeamInvitationStatusEnum.EXPIRED.getCode();
        }

        BasketTeam team = teamDao.selectById(invitation.getTeamId());
        BasketMemberProfile inviterProfile = memberProfileDao.selectOne(new LambdaQueryWrapper<BasketMemberProfile>()
                .eq(BasketMemberProfile::getMemberId, invitation.getInviterMemberId())
                .eq(BasketMemberProfile::getIsDelete, NOT_DELETED)
                .last("limit 1"));

        MessageTeamInviteInfoResp info = new MessageTeamInviteInfoResp();
        info.setInviteId(String.valueOf(invitation.getId()));
        info.setTeamId(String.valueOf(invitation.getTeamId()));
        info.setTeamName(team == null ? null : team.getTeamName());
        info.setTeamLogo(resolveLogoUrl(team == null ? null : team.getLogoFileId()));
        info.setInviteStatus(displayStatus);
        info.setInviteStatusText(resolveInviteStatusText(displayStatus));
        info.setInvitationMessage(invitation.getInvitationMessage());
        info.setInviterName(inviterProfile == null ? null : inviterProfile.getNickname());
        info.setInviteTime(invitation.getCreateTime());
        info.setExpireTime(invitation.getExpireTime());
        boolean canRespond = Objects.equals(TeamInvitationStatusEnum.PENDING.getCode(), displayStatus)
                && (invitation.getExpireTime() == null || invitation.getExpireTime().after(now));
        info.setCanRespond(canRespond);
        String joinIneligibilityReason = captainPermissionService.resolveJoinIneligibilityReason(memberId);
        info.setCanAccept(canRespond && joinIneligibilityReason == null);
        info.setCannotAcceptReason(canRespond ? joinIneligibilityReason : null);
        return info;
    }

    private String resolveLogoUrl(Long logoFileId) {
        if (logoFileId == null) {
            return null;
        }
        BasketFileAsset asset = fileAssetDao.selectById(logoFileId);
        if (asset == null || !NOT_DELETED.equals(asset.getIsDelete())) {
            return null;
        }
        return asset.getAccessUrl();
    }

    private String resolveInviteStatusText(Integer statusCode) {
        if (statusCode == null) {
            return "未知";
        }
        if (Objects.equals(TeamInvitationStatusEnum.PENDING.getCode(), statusCode)) {
            return "待处理";
        }
        if (Objects.equals(TeamInvitationStatusEnum.ACCEPTED.getCode(), statusCode)) {
            return "已接受";
        }
        if (Objects.equals(TeamInvitationStatusEnum.REJECTED.getCode(), statusCode)) {
            return "已拒绝";
        }
        if (Objects.equals(TeamInvitationStatusEnum.EXPIRED.getCode(), statusCode)) {
            return "已过期";
        }
        if (Objects.equals(TeamInvitationStatusEnum.CANCELLED.getCode(), statusCode)) {
            return "已撤销";
        }
        return TeamInvitationStatusEnum.fromCode(statusCode).getDescription();
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
