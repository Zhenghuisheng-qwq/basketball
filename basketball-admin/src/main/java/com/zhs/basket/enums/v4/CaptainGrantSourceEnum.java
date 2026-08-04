package com.zhs.basket.enums.v4;

public enum CaptainGrantSourceEnum {
    MANUAL("MANUAL", "人工"),
    PERSONAL_DRAW("PERSONAL_DRAW", "个人抽签");

    private final String code;
    private final String description;

    CaptainGrantSourceEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static CaptainGrantSourceEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (CaptainGrantSourceEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知CaptainGrantSourceEnum: " + code);
    }
}
