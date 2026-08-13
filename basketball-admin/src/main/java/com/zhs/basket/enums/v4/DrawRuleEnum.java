package com.zhs.basket.enums.v4;

public enum DrawRuleEnum {
    RANDOM_CONFIRM("RANDOM_CONFIRM", "Random draw confirm"),
    MANUAL_SLOT("MANUAL_SLOT", "Manual slot");

    private final String code;
    private final String description;

    DrawRuleEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static DrawRuleEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (DrawRuleEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown DrawRuleEnum: " + code);
    }
}
