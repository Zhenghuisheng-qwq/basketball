package com.zhs.enums;

public enum RosterLockStatusEnum {

    UNLOCKED(0, "未锁定"),
    LOCKED(1, "已锁定");

    private final int code;
    private final String description;

    RosterLockStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public boolean isLocked() {
        return this == LOCKED;
    }
}
