package com.zhs.basket.support.v4;

import com.zhs.basket.domain.BasketPlayerPlatformAccount;
import com.zhs.basket.enums.PlatformAccountStatusEnum;
import com.zhs.basket.support.GameTerminalSupport;
import com.zhs.common.utils.StringUtils;

import java.util.List;

/**
 * 赛事名单上的游戏 ID 以当前生效平台账号为准。平台账号主键没变、只改了游戏 ID 时，仍对上原来的那条账号。
 */
public final class CurrentEventGameId {

    private CurrentEventGameId() {
    }

    public static String resolve(Long platformIdentityId, String platformCode, List<BasketPlayerPlatformAccount> accounts) {
        if (accounts == null || accounts.isEmpty()) {
            return null;
        }
        String byIdentity = null;
        String byPlatform = null;
        for (BasketPlayerPlatformAccount account : accounts) {
            if (!usable(account)) {
                continue;
            }
            String gameId = account.getDisplayGameId().trim();
            if (byIdentity == null && platformIdentityId != null && platformIdentityId.equals(account.getId())) {
                byIdentity = gameId;
            }
            if (byPlatform == null && GameTerminalSupport.matchesDivisionPlatform(platformCode, account.getPlatform())) {
                byPlatform = gameId;
            }
        }
        return byIdentity != null ? byIdentity : byPlatform;
    }

    private static boolean usable(BasketPlayerPlatformAccount account) {
        if (account == null || !StringUtils.hasText(account.getDisplayGameId())) {
            return false;
        }
        Integer status = account.getAccountStatus();
        return status == null || status == PlatformAccountStatusEnum.ACTIVE.getCode();
    }
}
