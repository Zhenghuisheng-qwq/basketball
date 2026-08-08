package com.zhs.basket.enums.v4;

public enum QualificationTypeEnum {
    OVERALL_TOP_N("OVERALL_TOP_N", "总排名前N"),
    GROUP_TOP_N("GROUP_TOP_N", "小组前N"),
    MATCH_WINNER("MATCH_WINNER", "比赛胜者"),
    SERIES_WINNER("SERIES_WINNER", "系列赛胜者"),
    CHAMPION("CHAMPION", "冠军"),
    BYE_ADVANCE("BYE_ADVANCE", "轮空晋级"),
    MANUAL("MANUAL", "人工晋级");

    private final String code;
    private final String description;

    QualificationTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static QualificationTypeEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (QualificationTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知QualificationTypeEnum: " + code);
    }
}
