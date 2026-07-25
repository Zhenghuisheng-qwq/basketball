package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum RosterSourceTypeEnum {
    INITIAL_REGISTRATION("INITIAL_REGISTRATION", "初始报名"),
    OPEN_REGISTRATION("OPEN_REGISTRATION", "Open报名"),
    TRANSFER("TRANSFER", "转会"),
    FREE_AGENT_ADD("FREE_AGENT_ADD", "自由球员补员"),
    CAPTAIN_TRANSFER("CAPTAIN_TRANSFER", "队长变更"),
    ADMIN_OPERATION("ADMIN_OPERATION", "管理员操作");

    private final String code;
    private final String description;

    RosterSourceTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static RosterSourceTypeEnum fromCode(String code) {
        if (code == null) throw new ServiceException("未知阵容来源类型");
        for (RosterSourceTypeEnum v : values()) {
            if (v.code.equals(code)) return v;
        }
        throw new ServiceException("未知阵容来源类型");
    }
}
