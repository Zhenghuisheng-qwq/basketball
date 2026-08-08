package com.zhs.basket.enums.v4;

public enum StageTeamEntryTypeEnum {
    INITIAL("INITIAL", "初始入队"),
    OVERALL_RANK("OVERALL_RANK", "总排名"),
    GROUP_RANK("GROUP_RANK", "小组排名"),
    MATCH_WINNER("MATCH_WINNER", "比赛胜者"),
    SERIES_WINNER("SERIES_WINNER", "系列赛胜者"),
    BYE_ADVANCE("BYE_ADVANCE", "轮空晋级"),
    MANUAL("MANUAL", "人工晋级");

    private final String code;
    private final String description;

    StageTeamEntryTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static StageTeamEntryTypeEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (StageTeamEntryTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知StageTeamEntryTypeEnum: " + code);
    }
}
