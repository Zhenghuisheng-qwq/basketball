package com.zhs.basket.enums.v4;

public enum PairingRuleEnum {
    NONE("NONE", "无"),
    HIGH_VS_LOW("HIGH_VS_LOW", "高排名对低排名"),
    RANDOM_DRAW("RANDOM_DRAW", "随机抽签"),
    /** 季后赛固定淘汰树：首轮象限落位，后续轮继承相邻 Series Winner。 */
    PLAYOFF_BRACKET("PLAYOFF_BRACKET", "季后赛固定淘汰树"),
    GROUP_CROSS("GROUP_CROSS", "小组交叉"),
    MANUAL("MANUAL", "人工对阵");

    private final String code;
    private final String description;

    PairingRuleEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PairingRuleEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (PairingRuleEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知PairingRuleEnum: " + code);
    }
}
