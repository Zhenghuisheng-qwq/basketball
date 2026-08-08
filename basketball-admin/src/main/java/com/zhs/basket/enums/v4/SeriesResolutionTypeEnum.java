package com.zhs.basket.enums.v4;

/**
 * 系列赛完成原因（与 series_status=COMPLETED 配合，不替代生命周期状态）。
 */
public enum SeriesResolutionTypeEnum {
    NORMAL("NORMAL", "正常"),
    FORFEIT("FORFEIT", "弃赛"),
    MANUAL("MANUAL", "人工判定");

    private final String code;
    private final String description;

    SeriesResolutionTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static SeriesResolutionTypeEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (SeriesResolutionTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知SeriesResolutionTypeEnum: " + code);
    }
}
