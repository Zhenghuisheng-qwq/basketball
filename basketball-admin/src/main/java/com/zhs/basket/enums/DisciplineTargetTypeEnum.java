package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum DisciplineTargetTypeEnum {
    PLAYER("PLAYER"), PERMANENT_TEAM("PERMANENT_TEAM"), EVENT_TEAM("EVENT_TEAM"), ACCOUNT("ACCOUNT");

    private final String code;
    DisciplineTargetTypeEnum(String code) { this.code = code; }
    public String getCode() { return code; }
    public static DisciplineTargetTypeEnum fromCode(String code) {
        for (DisciplineTargetTypeEnum v : values()) if (v.code.equalsIgnoreCase(code)) return v;
        throw new ServiceException("未知处罚对象类型");
    }
}
