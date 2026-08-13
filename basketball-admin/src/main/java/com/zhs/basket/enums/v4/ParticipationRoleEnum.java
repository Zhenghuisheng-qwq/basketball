package com.zhs.basket.enums.v4;

public enum ParticipationRoleEnum {
    UNALLOCATED("UNALLOCATED", "未分配"),
    FORMAL("FORMAL", "正式参赛"),
    CANDIDATE("CANDIDATE", "候补球队"),
    EXCLUDED("EXCLUDED", "本届不参赛");

    private final String code;
    private final String description;

    ParticipationRoleEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ParticipationRoleEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return UNALLOCATED;
        }
        for (ParticipationRoleEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知ParticipationRoleEnum: " + code);
    }
}
