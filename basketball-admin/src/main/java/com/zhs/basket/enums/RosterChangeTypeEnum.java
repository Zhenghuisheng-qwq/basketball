package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum RosterChangeTypeEnum {
    TRANSFER("TRANSFER", "转会"),
    FREE_AGENT_ADD("FREE_AGENT_ADD", "自由球员补员"),
    CAPTAIN_TRANSFER("CAPTAIN_TRANSFER", "队长变更");

    private final String code;
    private final String description;

    RosterChangeTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static RosterChangeTypeEnum fromCode(String code) {
        if (code == null) throw new ServiceException("未知阵容变更类型");
        for (RosterChangeTypeEnum v : values()) {
            if (v.code.equals(code)) return v;
        }
        throw new ServiceException("未知阵容变更类型");
    }
}
