package com.zhs.basket.enums.v4;

/**
 * CNPA 赛事域 V4 枚举：GenerateStatusEnum（赛程批次生成状态）
 */
public enum GenerateStatusEnum {
    DRAFT("DRAFT", "草稿"),
    SUCCESS("SUCCESS", "生成成功"),
    FAILED("FAILED", "生成失败");

    private final String code;
    private final String description;

    GenerateStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static GenerateStatusEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (GenerateStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知GenerateStatusEnum: " + code);
    }
}
