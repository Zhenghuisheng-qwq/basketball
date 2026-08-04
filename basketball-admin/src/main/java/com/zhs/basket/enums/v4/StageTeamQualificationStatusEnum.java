package com.zhs.basket.enums.v4;

public enum StageTeamQualificationStatusEnum {
    PENDING("PENDING", "待定"),
    QUALIFIED("QUALIFIED", "已晋级"),
    ELIMINATED("ELIMINATED", "已淘汰"),
    CHAMPION("CHAMPION", "冠军");

    private final String code;
    private final String description;

    StageTeamQualificationStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static StageTeamQualificationStatusEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (StageTeamQualificationStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知StageTeamQualificationStatusEnum: " + code);
    }
}
