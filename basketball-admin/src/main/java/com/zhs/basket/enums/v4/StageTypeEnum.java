package com.zhs.basket.enums.v4;

public enum StageTypeEnum {
    REGULAR("REGULAR", "常规赛"),
    GROUP("GROUP", "小组赛"),
    KNOCKOUT("KNOCKOUT", "淘汰赛"),
    SEMI_FINAL("SEMI_FINAL", "半决赛"),
    FINAL("FINAL", "总决赛"),
    CUSTOM("CUSTOM", "自定义");

    private final String code;
    private final String description;

    StageTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static StageTypeEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (StageTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知StageTypeEnum: " + code);
    }
}
