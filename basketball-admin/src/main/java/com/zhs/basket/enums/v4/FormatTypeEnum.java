package com.zhs.basket.enums.v4;

/**
 * CNPA 赛事域 V4 枚举：FormatTypeEnum
 * 存储方式：VARCHAR 字符串 code，与旧整型枚举隔离。
 */
public enum FormatTypeEnum {
    REGULAR_ROUND_ROBIN("REGULAR_ROUND_ROBIN", "常规赛单循环"),
    REGULAR_PLAYOFF("REGULAR_PLAYOFF", "常规赛+季后赛"),
    GROUP_KNOCKOUT("GROUP_KNOCKOUT", "小组赛+淘汰赛"),
    KNOCKOUT("KNOCKOUT", "单淘汰");

    private final String code;
    private final String description;

    FormatTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static FormatTypeEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (FormatTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知FormatTypeEnum: " + code);
    }
}
