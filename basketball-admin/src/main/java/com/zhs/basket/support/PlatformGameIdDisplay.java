package com.zhs.basket.support;

import com.zhs.basket.domain.BasketPlayerPlatformAccount;
import com.zhs.common.utils.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 按赛区平台展示已通过认证的游戏 ID。球员主键和球员编号不作为 Game ID。
 */
public final class PlatformGameIdDisplay {

    private PlatformGameIdDisplay() {
    }

    public static String format(String divisionPlatform, List<BasketPlayerPlatformAccount> accounts) {
        if (accounts == null || accounts.isEmpty()) {
            return null;
        }
        List<BasketPlayerPlatformAccount> matched = new ArrayList<>();
        for (BasketPlayerPlatformAccount account : accounts) {
            if (account == null || !StringUtils.hasText(account.getDisplayGameId())) {
                continue;
            }
            if (StringUtils.hasText(divisionPlatform)
                    && !GameTerminalSupport.matchesDivisionPlatform(divisionPlatform, account.getPlatform())) {
                continue;
            }
            matched.add(account);
        }
        if (matched.isEmpty()) {
            return null;
        }
        if (matched.size() == 1) {
            return matched.get(0).getDisplayGameId().trim();
        }
        Set<String> labels = new LinkedHashSet<>();
        for (BasketPlayerPlatformAccount account : matched) {
            labels.add(shortPlatform(account.getPlatform()) + " " + account.getDisplayGameId().trim());
        }
        return String.join(" / ", labels);
    }

    private static String shortPlatform(String platform) {
        String normalized = GameTerminalSupport.normalize(platform);
        if ("XBOX".equals(normalized)) {
            return "Xbox";
        }
        if ("PS".equals(normalized)) {
            return "PS";
        }
        return normalized == null ? "平台" : normalized;
    }
}
