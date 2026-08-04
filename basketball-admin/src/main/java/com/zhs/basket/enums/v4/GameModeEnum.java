package com.zhs.basket.enums.v4;

import com.zhs.basket.domain.v4.RosterLimit;

/**
 * CNPA 赛事域 V4 枚举：GameModeEnum
 */
public enum GameModeEnum {
    FIVE_V_FIVE("FIVE_V_FIVE", "5V5", 5, 8),
    THREE_V_THREE("THREE_V_THREE", "3V3", 3, 8);

    private final String code;
    private final String description;
    private final int minRosterCount;
    private final int maxRosterCount;

    GameModeEnum(String code, String description, int minRosterCount, int maxRosterCount) {
        this.code = code;
        this.description = description;
        this.minRosterCount = minRosterCount;
        this.maxRosterCount = maxRosterCount;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public int getMinRosterCount() {
        return minRosterCount;
    }

    public int getMaxRosterCount() {
        return maxRosterCount;
    }

    public RosterLimit toRosterLimit() {
        return new RosterLimit(minRosterCount, maxRosterCount);
    }

    public static GameModeEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (GameModeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知GameModeEnum: " + code);
    }

    public static GameModeEnum requireCode(String code) {
        GameModeEnum mode = fromCode(code);
        if (mode == null) {
            throw new IllegalArgumentException("gameMode 不能为空");
        }
        return mode;
    }
}
