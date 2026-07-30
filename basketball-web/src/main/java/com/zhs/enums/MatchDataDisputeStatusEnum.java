package com.zhs.enums;

import com.zhs.exception.ServiceException;

public enum MatchDataDisputeStatusEnum {

    PENDING("PENDING", "待处理"),
    PROCESSING("PROCESSING", "处理中"),
    REJECTED("REJECTED", "已驳回"),
    UPHELD("UPHELD", "已采纳");

    private final String code;
    private final String description;

    MatchDataDisputeStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return this == PENDING || this == PROCESSING;
    }

    public static MatchDataDisputeStatusEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ServiceException("争议状态不能为空");
        }
        for (MatchDataDisputeStatusEnum value : values()) {
            if (value.code.equalsIgnoreCase(code.trim())) {
                return value;
            }
        }
        throw new ServiceException("不支持的争议状态");
    }
}
