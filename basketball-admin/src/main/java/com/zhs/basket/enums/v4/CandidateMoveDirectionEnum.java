package com.zhs.basket.enums.v4;

public enum CandidateMoveDirectionEnum {
    UP("UP", "上移"),
    DOWN("DOWN", "下移");

    private final String code;
    private final String description;

    CandidateMoveDirectionEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static CandidateMoveDirectionEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("移动方向不能为空");
        }
        for (CandidateMoveDirectionEnum value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知移动方向: " + code);
    }
}
