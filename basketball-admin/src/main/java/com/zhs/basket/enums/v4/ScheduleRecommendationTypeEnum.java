package com.zhs.basket.enums.v4;

public enum ScheduleRecommendationTypeEnum {
    ROUND_ROBIN_STANDARD("ROUND_ROBIN_STANDARD", "循环赛标准推荐"),
    MANUAL_ONLY("MANUAL_ONLY", "仅手工排期");

    private final String code;
    private final String description;

    ScheduleRecommendationTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static ScheduleRecommendationTypeEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (ScheduleRecommendationTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown ScheduleRecommendationTypeEnum: " + code);
    }
}
