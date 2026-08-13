package com.zhs.basket.enums.v4;

public enum ParticipantSourceTypeEnum {
    DIRECT_EVENT_TEAM("DIRECT_EVENT_TEAM", "Formal event team"),
    GROUP_RANK("GROUP_RANK", "Group rank source"),
    BYE("BYE", "Bye"),
    WINNER_OF_SERIES("WINNER_OF_SERIES", "Series winner");

    private final String code;
    private final String description;

    ParticipantSourceTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ParticipantSourceTypeEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (ParticipantSourceTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown ParticipantSourceTypeEnum: " + code);
    }
}
