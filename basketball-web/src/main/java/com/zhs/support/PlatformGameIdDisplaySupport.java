package com.zhs.support;

import com.zhs.enums.PlatformAccountStatusEnum;
import com.zhs.model.BasketPlayerPlatformAccount;
import com.zhs.response.common.PlatformGameIdItemResp;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 平台游戏 ID 展示辅助（只读）。
 */
public final class PlatformGameIdDisplaySupport {

    public static final String UNBOUND = "未绑定";

    private PlatformGameIdDisplaySupport() {
    }

    public static List<PlatformGameIdItemResp> toItems(Collection<BasketPlayerPlatformAccount> accounts) {
        if (CollectionUtils.isEmpty(accounts)) {
            return Collections.emptyList();
        }
        Map<String, PlatformGameIdItemResp> ordered = new LinkedHashMap<>();
        accounts.stream()
                .filter(Objects::nonNull)
                .filter(a -> Objects.equals(PlatformAccountStatusEnum.ACTIVE.getCode(), a.getAccountStatus()))
                .filter(a -> StringUtils.hasText(a.getDisplayGameId()))
                .sorted(Comparator
                        .comparing((BasketPlayerPlatformAccount a) -> a.getIsPrimary() == null ? 0 : a.getIsPrimary())
                        .reversed()
                        .thenComparing(a -> normalizePlatform(a.getPlatform())))
                .forEach(account -> {
                    String platform = normalizePlatform(account.getPlatform());
                    if (!ordered.containsKey(platform)) {
                        PlatformGameIdItemResp item = new PlatformGameIdItemResp();
                        item.setPlatformCode(platform);
                        item.setGameId(account.getDisplayGameId().trim());
                        ordered.put(platform, item);
                    }
                });
        return new ArrayList<>(ordered.values());
    }

    public static String summary(Collection<BasketPlayerPlatformAccount> accounts) {
        List<PlatformGameIdItemResp> items = toItems(accounts);
        if (items.isEmpty()) {
            return UNBOUND;
        }
        return items.stream()
                .map(item -> item.getPlatformCode() + "：" + item.getGameId())
                .collect(Collectors.joining(" / "));
    }

    public static String normalizePlatform(String platform) {
        if (!StringUtils.hasText(platform)) {
            return "未知";
        }
        String normalized = platform.trim().toUpperCase(Locale.ROOT);
        if ("PS5".equals(normalized) || "PSN".equals(normalized) || "PLAYSTATION".equals(normalized)) {
            return "PS";
        }
        if ("XBOXONE".equals(normalized) || "XB".equals(normalized) || "XBOX".equals(normalized)) {
            return "Xbox";
        }
        if ("PC".equals(normalized) || "STEAM".equals(normalized)) {
            return "PC";
        }
        return normalized;
    }
}
