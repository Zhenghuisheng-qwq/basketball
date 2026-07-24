package com.zhs.basket.enums;

public enum DisciplineStatusEnum {
    INACTIVE(0), ACTIVE(1);
    private final int code;
    DisciplineStatusEnum(int code) { this.code = code; }
    public int getCode() { return code; }
}
