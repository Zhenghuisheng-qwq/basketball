package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum MatchResultTypeEnum {
    WIN("WIN", "胜"),
    LOSS("LOSS", "负"),
    FORFEIT_WIN("FORFEIT_WIN", "判胜"),
    FORFEIT_LOSS("FORFEIT_LOSS", "判负"),
    NONE("NONE", "无");

    private final String code;
    private final String description;

    MatchResultTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static MatchResultTypeEnum fromCode(String code) {
        if (code == null || code.isBlank()) throw new ServiceException("未知比赛结果");
        for (MatchResultTypeEnum v : values()) {
            if (v.code.equalsIgnoreCase(code.trim())) return v;
        }
        throw new ServiceException("未知比赛结果");
    }
}
