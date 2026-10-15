package com.zhs.basket.enums.v4;

public enum CaptainGrantValidToModeEnum {
    AUTO_EVENT("AUTO_EVENT", "跟随赛事"),
    CUSTOM("CUSTOM", "手动指定");

    private final String code;
    private final String description;

    CaptainGrantValidToModeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static CaptainGrantValidToModeEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (CaptainGrantValidToModeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
