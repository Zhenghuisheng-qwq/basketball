package com.zhs.basket.enums.v4;

public enum ByeRuleEnum {
    SEED_PRIORITY("SEED_PRIORITY", "Seed priority bye"),
    RANDOM("RANDOM", "Random bye"),
    MANUAL("MANUAL", "Manual bye");

    private final String code;
    private final String description;

    ByeRuleEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static ByeRuleEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (ByeRuleEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown ByeRuleEnum: " + code);
    }
}
