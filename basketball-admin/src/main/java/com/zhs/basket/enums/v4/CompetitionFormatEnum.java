package com.zhs.basket.enums.v4;

public enum CompetitionFormatEnum {
    ROUND_ROBIN("ROUND_ROBIN", "单循环/循环赛"),
    SINGLE_ELIMINATION("SINGLE_ELIMINATION", "单败淘汰"),
    MANUAL("MANUAL", "人工安排");

    private final String code;
    private final String description;

    CompetitionFormatEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static CompetitionFormatEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (CompetitionFormatEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知CompetitionFormatEnum: " + code);
    }
}
