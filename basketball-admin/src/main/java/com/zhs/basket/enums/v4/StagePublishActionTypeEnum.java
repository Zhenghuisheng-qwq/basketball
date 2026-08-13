package com.zhs.basket.enums.v4;

public enum StagePublishActionTypeEnum {
    PUBLISH("PUBLISH", "发布"),
    WITHDRAW("WITHDRAW", "撤回"),
    REPUBLISH("REPUBLISH", "再发布");

    private final String code;
    private final String description;

    StagePublishActionTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static StagePublishActionTypeEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (StagePublishActionTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown StagePublishActionTypeEnum: " + code);
    }
}
