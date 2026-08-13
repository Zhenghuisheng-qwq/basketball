package com.zhs.basket.enums.v4;

public enum PairingExecutionStatusEnum {
    DRAFT("DRAFT", "Draft"),
    CONFIRMED("CONFIRMED", "Confirmed"),
    SUPERSEDED("SUPERSEDED", "Superseded");

    private final String code;
    private final String description;

    PairingExecutionStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PairingExecutionStatusEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (PairingExecutionStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown PairingExecutionStatusEnum: " + code);
    }
}
