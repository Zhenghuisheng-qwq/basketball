package com.zhs.basket.support.v4;

import com.zhs.basket.domain.BasketPlayerPlatformAccount;
import com.zhs.basket.mapper.BasketPlayerPlatformAccountMapper;
import com.zhs.basket.mapper.v4.BasketEventPlayerMapper;
import com.zhs.basket.mapper.v4.BasketPersonalRegistrationMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationPlayerMapper;
import com.zhs.basket.support.GameTerminalSupport;
import com.zhs.basket.vo.v4.BasketEventPlayerListVo;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台游戏 ID 审核通过后，把新 ID 写回仍在赛事里的名单。
 * 报名阵容和单人报名一并更新，避免之后的阵容同步再把旧 ID 抄回去。
 */
@Component
public class EventGameIdSnapshotSync {

    @Autowired
    private BasketEventPlayerMapper eventPlayerMapper;
    @Autowired
    private BasketRegistrationPlayerMapper registrationPlayerMapper;
    @Autowired
    private BasketPersonalRegistrationMapper personalRegistrationMapper;
    @Autowired
    private BasketPlayerPlatformAccountMapper platformAccountMapper;

    public void syncApprovedChange(BasketPlayerPlatformAccount account) {
        if (account == null || account.getId() == null || account.getPlayerId() == null
                || !StringUtils.hasText(account.getDisplayGameId())) {
            return;
        }
        String gameId = account.getDisplayGameId().trim();
        String platform = GameTerminalSupport.normalize(account.getPlatform());
        eventPlayerMapper.syncGameIdByPlatformAccount(account.getPlayerId(), account.getId(), platform, gameId);
        registrationPlayerMapper.syncGameIdByPlatformAccount(account.getPlayerId(), account.getId(), platform, gameId);
        personalRegistrationMapper.syncGameAccountByPlatformAccount(account.getPlayerId(), account.getId(), platform, gameId);
    }

    public void applyEventPlayerList(List<BasketEventPlayerListVo> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<Long> playerIds = new ArrayList<>();
        for (BasketEventPlayerListVo row : rows) {
            if (row != null && row.getSourcePlayerId() != null) {
                playerIds.add(row.getSourcePlayerId());
            }
        }
        Map<Long, List<BasketPlayerPlatformAccount>> accounts = loadAccounts(playerIds);
        for (BasketEventPlayerListVo row : rows) {
            if (row == null || row.getSourcePlayerId() == null) {
                continue;
            }
            String current = CurrentEventGameId.resolve(
                    row.getPlatformIdentityId(), row.getPlatformCode(), accounts.get(row.getSourcePlayerId()));
            if (StringUtils.hasText(current)) {
                row.setGameIdSnapshot(current);
            }
        }
    }

    public Map<Long, List<BasketPlayerPlatformAccount>> loadAccounts(Collection<Long> playerIds) {
        Map<Long, List<BasketPlayerPlatformAccount>> grouped = new HashMap<>();
        if (playerIds == null || playerIds.isEmpty()) {
            return grouped;
        }
        List<Long> ids = new ArrayList<>();
        for (Long playerId : playerIds) {
            if (playerId != null && !ids.contains(playerId)) {
                ids.add(playerId);
            }
        }
        if (ids.isEmpty()) {
            return grouped;
        }
        List<BasketPlayerPlatformAccount> accounts = platformAccountMapper.selectActiveByPlayerIds(ids);
        if (accounts == null) {
            return grouped;
        }
        for (BasketPlayerPlatformAccount account : accounts) {
            if (account == null || account.getPlayerId() == null) {
                continue;
            }
            grouped.computeIfAbsent(account.getPlayerId(), key -> new ArrayList<>()).add(account);
        }
        return grouped;
    }
}
