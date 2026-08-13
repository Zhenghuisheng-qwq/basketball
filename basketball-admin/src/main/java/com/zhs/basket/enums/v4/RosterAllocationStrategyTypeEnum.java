package com.zhs.basket.enums.v4;

public enum RosterAllocationStrategyTypeEnum {
    AUTO_CAPACITY("AUTO_CAPACITY", "按正式名额自动分配");

    private final String code;
    private final String description;

    RosterAllocationStrategyTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
