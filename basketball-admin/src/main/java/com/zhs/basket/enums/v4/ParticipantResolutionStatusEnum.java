package com.zhs.basket.enums.v4;

public enum ParticipantResolutionStatusEnum {
    PENDING("PENDING", "Pending"),
    RESOLVED("RESOLVED", "Resolved"),
    BYE("BYE", "Bye not required");

    private final String code;
    private final String description;

    ParticipantResolutionStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ParticipantResolutionStatusEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (ParticipantResolutionStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown ParticipantResolutionStatusEnum: " + code);
    }
}
