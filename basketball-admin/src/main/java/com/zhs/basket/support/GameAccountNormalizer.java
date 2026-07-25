package com.zhs.basket.support;

import com.zhs.common.utils.StringUtils;

/**
 * 游戏账号规范化：trim + 小写，用于唯一性校验；展示值保留原样。
 */
public final class GameAccountNormalizer {

    private GameAccountNormalizer() {
    }

    public static String normalize(String displayGameId) {
        if (!StringUtils.hasText(displayGameId)) {
            return null;
        }
        return displayGameId.trim().toLowerCase();
    }

    public static String display(String displayGameId) {
        if (!StringUtils.hasText(displayGameId)) {
            return null;
        }
        return displayGameId.trim();
    }
}
