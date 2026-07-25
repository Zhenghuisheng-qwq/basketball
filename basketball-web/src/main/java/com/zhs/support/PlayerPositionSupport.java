package com.zhs.support;

import org.springframework.util.StringUtils;

import java.util.Map;

public final class PlayerPositionSupport {

    private static final Map<String, String> LABELS = Map.of(
            "PG", "控球后卫",
            "SG", "得分后卫",
            "SF", "小前锋",
            "PF", "大前锋",
            "C", "中锋"
    );

    private PlayerPositionSupport() {
    }

    public static String resolveName(String positionCode) {
        if (!StringUtils.hasText(positionCode)) {
            return null;
        }
        return LABELS.get(positionCode.trim().toUpperCase());
    }
}
