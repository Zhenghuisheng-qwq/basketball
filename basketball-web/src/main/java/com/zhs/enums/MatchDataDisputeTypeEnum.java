package com.zhs.enums;

import com.zhs.exception.ServiceException;

public enum MatchDataDisputeTypeEnum {

    SCORE("SCORE", "比分争议"),
    STATS("STATS", "技术统计争议"),
    OTHER("OTHER", "其他争议");

    private final String code;
    private final String description;

    MatchDataDisputeTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static MatchDataDisputeTypeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ServiceException("争议类型不能为空");
        }
        for (MatchDataDisputeTypeEnum value : values()) {
            if (value.code.equalsIgnoreCase(code.trim())) {
                return value;
            }
        }
        throw new ServiceException("不支持的争议类型");
    }
}
