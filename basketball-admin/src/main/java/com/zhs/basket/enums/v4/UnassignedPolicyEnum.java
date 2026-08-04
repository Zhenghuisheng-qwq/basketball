package com.zhs.basket.enums.v4;

public enum UnassignedPolicyEnum {
    KEEP_FOR_NEXT("KEEP_FOR_NEXT", "留待下批"),
    DISQUALIFY("DISQUALIFY", "取消资格"),
    REMARK("REMARK", "仅备注");

    private final String code;
    private final String description;

    UnassignedPolicyEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static UnassignedPolicyEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (UnassignedPolicyEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知UnassignedPolicyEnum: " + code);
    }
}
