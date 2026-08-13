package com.zhs.basket.enums.v4;

public enum ValidationSeverityEnum {
    ERROR("ERROR", "阻止"),
    WARNING("WARNING", "警告");

    private final String code;
    private final String description;

    ValidationSeverityEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static ValidationSeverityEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (ValidationSeverityEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown ValidationSeverityEnum: " + code);
    }
}
