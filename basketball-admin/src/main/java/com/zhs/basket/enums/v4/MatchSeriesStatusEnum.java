package com.zhs.basket.enums.v4;

/**
 * CNPA 赛事域 V4 枚举：MatchSeriesStatusEnum
 * BO3/BO5 系列赛状态。
 */
public enum MatchSeriesStatusEnum {
    PENDING("PENDING", "待准备"),
    READY("READY", "可开始"),
    RUNNING("RUNNING", "进行中"),
    COMPLETED("COMPLETED", "已结束"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String description;

    MatchSeriesStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static MatchSeriesStatusEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (MatchSeriesStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知MatchSeriesStatusEnum: " + code);
    }
}
