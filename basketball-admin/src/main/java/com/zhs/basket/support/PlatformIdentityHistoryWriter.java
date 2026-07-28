package com.zhs.basket.support;

import com.zhs.basket.domain.BasketPlayerPlatformAccount;
import com.zhs.basket.domain.BasketPlayerPlatformChangeRequest;
import com.zhs.basket.domain.BasketPlayerPlatformIdentityHistory;
import com.zhs.basket.enums.PlatformIdentityHistoryChangeTypeEnum;
import com.zhs.basket.mapper.BasketPlayerPlatformIdentityHistoryMapper;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class PlatformIdentityHistoryWriter {

    private static final Integer NOT_DELETED = 0;

    @Autowired
    private BasketPlayerPlatformIdentityHistoryMapper historyMapper;

    public void writeInitialBind(Long userId, Long playerId, BasketPlayerPlatformAccount account,
                                 Date effectiveTime, Long auditRecordId, Long operatorId) {
        if (account == null || account.getId() == null) {
            throw new ServiceException("平台身份不存在，无法写入变更历史");
        }
        String platformCode = normalizePlatformCode(account.getPlatform());
        if (historyMapper.countInitialBind(playerId, platformCode) > 0) {
            return;
        }
        BasketPlayerPlatformIdentityHistory history = baseHistory(userId, playerId, account.getId(), platformCode,
                PlatformIdentityHistoryChangeTypeEnum.INITIAL_BIND.getCode(), null, account.getDisplayGameId(),
                effectiveTime, auditRecordId, operatorId);
        history.setInitialBindSlot(playerId + ":" + platformCode);
        insertHistory(history);
    }

    public void writeUpdateGameId(Long userId, Long playerId, Long platformIdentityId, String platformCode,
                                  String oldGameId, String newGameId, Date effectiveTime,
                                  Long auditRecordId, Long operatorId) {
        if (!StringUtils.hasText(newGameId)) {
            throw new ServiceException("新游戏 ID 不能为空");
        }
        if (StringUtils.hasText(oldGameId) && oldGameId.trim().equalsIgnoreCase(newGameId.trim())) {
            return;
        }
        BasketPlayerPlatformIdentityHistory history = baseHistory(userId, playerId, platformIdentityId,
                normalizePlatformCode(platformCode), PlatformIdentityHistoryChangeTypeEnum.UPDATE_GAME_ID.getCode(),
                oldGameId, newGameId, effectiveTime, auditRecordId, operatorId);
        insertHistory(history);
    }

    public void writeApprovedPlatformChange(BasketPlayerPlatformChangeRequest request,
                                            BasketPlayerPlatformAccount account,
                                            String oldDisplayGameId,
                                            Long operatorId,
                                            Date effectiveTime) {
        String historyChangeType = PlatformIdentityHistoryChangeTypeEnum.mapRequestChangeType(request.getChangeType());
        if (PlatformIdentityHistoryChangeTypeEnum.UPDATE_GAME_ID.getCode().equals(historyChangeType)) {
            writeUpdateGameId(request.getMemberId(), request.getPlayerId(), account.getId(), account.getPlatform(),
                    oldDisplayGameId, account.getDisplayGameId(), effectiveTime, request.getId(), operatorId);
            return;
        }
        writeInitialBind(request.getMemberId(), request.getPlayerId(), account, effectiveTime,
                request.getId(), operatorId);
    }

    private BasketPlayerPlatformIdentityHistory baseHistory(Long userId, Long playerId, Long platformIdentityId,
                                                            String platformCode, String changeType,
                                                            String oldGameId, String newGameId,
                                                            Date effectiveTime, Long auditRecordId, Long operatorId) {
        BasketPlayerPlatformIdentityHistory history = new BasketPlayerPlatformIdentityHistory();
        history.setUserId(userId);
        history.setPlayerId(playerId);
        history.setPlatformIdentityId(platformIdentityId);
        history.setPlatformCode(platformCode);
        history.setChangeType(changeType);
        history.setOldGameId(oldGameId);
        history.setNewGameId(newGameId);
        history.setEffectiveTime(effectiveTime);
        history.setAuditRecordId(auditRecordId);
        history.setOperatorId(operatorId);
        history.setCreateTime(effectiveTime != null ? effectiveTime : new Date());
        history.setIsDelete(NOT_DELETED);
        return history;
    }

    private void insertHistory(BasketPlayerPlatformIdentityHistory history) {
        try {
            if (historyMapper.insert(history) != 1) {
                throw new ServiceException("写入平台游戏 ID 变更历史失败");
            }
        } catch (DuplicateKeyException ex) {
            if (history.getInitialBindSlot() != null) {
                return;
            }
            throw new ServiceException("写入平台游戏 ID 变更历史失败");
        }
    }

    private String normalizePlatformCode(String platform) {
        if (!StringUtils.hasText(platform)) {
            throw new ServiceException("平台代码不能为空");
        }
        String normalized = platform.trim().toUpperCase();
        if ("PS5".equals(normalized) || "PSN".equals(normalized)) {
            return "PS";
        }
        return normalized;
    }
}
