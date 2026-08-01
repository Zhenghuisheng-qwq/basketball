package com.zhs.basket.support;

import com.zhs.basket.domain.BasketPlayerPlatformAccount;
import com.zhs.basket.enums.PlatformAccountStatusEnum;
import com.zhs.basket.vo.PlatformGameIdItemVo;
import com.zhs.common.utils.StringUtils;

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
 * 平台游戏 ID 展示辅助（只读，不改业务规则）。
 */
public final class PlatformGameIdDisplaySupport {

    public static final String UNBOUND = "未绑定";
    private static final int COMPACT_THRESHOLD = 2;

    private PlatformGameIdDisplaySupport() {
    }

    public static List<PlatformGameIdItemVo> toItems(Collection<BasketPlayerPlatformAccount> accounts) {
        if (accounts == null || accounts.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, PlatformGameIdItemVo> ordered = new LinkedHashMap<>();
        accounts.stream()
                .filter(Objects::nonNull)
                .filter(a -> Objects.equals(PlatformAccountStatusEnum.ACTIVE.getCode(), a.getAccountStatus()))
                .filter(a -> StringUtils.isNotEmpty(a.getDisplayGameId()))
                .sorted(Comparator
                        .comparing((BasketPlayerPlatformAccount a) -> a.getIsPrimary() == null ? 0 : a.getIsPrimary())
                        .reversed()
                        .thenComparing(a -> normalizePlatform(a.getPlatform())))
                .forEach(account -> {
                    String platform = normalizePlatform(account.getPlatform());
                    if (!ordered.containsKey(platform)) {
                        PlatformGameIdItemVo item = new PlatformGameIdItemVo();
                        item.setPlatformCode(platform);
                        item.setGameId(account.getDisplayGameId().trim());
                        ordered.put(platform, item);
                    }
                });
        return new ArrayList<>(ordered.values());
    }

    public static String summary(Collection<BasketPlayerPlatformAccount> accounts) {
        List<PlatformGameIdItemVo> items = toItems(accounts);
        if (items.isEmpty()) {
            return UNBOUND;
        }
        return items.stream()
                .map(item -> item.getPlatformCode() + "：" + item.getGameId())
                .collect(Collectors.joining(" / "));
    }

    public static String compactSummary(Collection<BasketPlayerPlatformAccount> accounts) {
        List<PlatformGameIdItemVo> items = toItems(accounts);
        if (items.isEmpty()) {
            return UNBOUND;
        }
        if (items.size() <= COMPACT_THRESHOLD) {
            return summary(accounts);
        }
        String platforms = items.stream().map(PlatformGameIdItemVo::getPlatformCode).collect(Collectors.joining(" / "));
        return "已绑定 " + items.size() + " 个：" + platforms;
    }

    public static String normalizePlatform(String platform) {
        if (!StringUtils.isNotEmpty(platform)) {
            return "未知";
        }
        String normalized = platform.trim().toUpperCase(Locale.ROOT);
        if ("PS5".equals(normalized) || "PSN".equals(normalized) || "PLAYSTATION".equals(normalized)) {
            return "PS";
        }
        if ("XBOXONE".equals(normalized) || "XBOX SERIES".equals(normalized) || "XB".equals(normalized)) {
            return "Xbox";
        }
        if ("XBOX".equals(normalized)) {
            return "Xbox";
        }
        if ("PC".equals(normalized) || "STEAM".equals(normalized)) {
            return "PC";
        }
        return normalized;
    }
}
