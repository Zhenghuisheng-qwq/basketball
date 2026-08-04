package com.zhs.basket.enums.v4;

public enum FormationStatusEnum {
    DRAFT("DRAFT", "草稿"),
    CAPTAIN_ASSIGNED("CAPTAIN_ASSIGNED", "已指定队长"),
    TEAM_FORMING("TEAM_FORMING", "组队中"),
    READY_TO_CONFIRM("READY_TO_CONFIRM", "待确认"),
    CONFIRMED("CONFIRMED", "已确认"),
    CONVERTED("CONVERTED", "已转换"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String description;

    FormationStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static FormationStatusEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (FormationStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知FormationStatusEnum: " + code);
    }
}
