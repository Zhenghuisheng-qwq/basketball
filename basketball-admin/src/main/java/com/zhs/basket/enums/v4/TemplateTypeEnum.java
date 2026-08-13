package com.zhs.basket.enums.v4;

public enum TemplateTypeEnum {
    STANDARD("STANDARD", "Standard template"),
    CUSTOM("CUSTOM", "Custom template");

    private final String code;
    private final String description;

    TemplateTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static TemplateTypeEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (TemplateTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown TemplateTypeEnum: " + code);
    }

    public boolean isStandard() {
        return this == STANDARD;
    }
}
