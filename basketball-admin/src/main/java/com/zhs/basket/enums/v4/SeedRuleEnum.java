package com.zhs.basket.enums.v4;

public enum SeedRuleEnum {
    NONE("NONE", "No seed"),
    ADMIN("ADMIN", "Admin seed"),
    ADMIN_SEED("ADMIN_SEED", "Admin seed"),
    RANDOM("RANDOM", "Random seed"),
    EXTERNAL_RANKING("EXTERNAL_RANKING", "External ranking");

    private final String code;
    private final String description;

    SeedRuleEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static SeedRuleEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (SeedRuleEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown SeedRuleEnum: " + code);
    }
}
