package com.zhs.enums;

import com.zhs.exception.ServiceException;

public enum MatchExceptionApplicationStatusEnum {

    PENDING("PENDING", "待处理"),
    PROCESSING("PROCESSING", "处理中"),
    APPROVED("APPROVED", "已通过"),
    REJECTED("REJECTED", "已驳回");

    private final String code;
    private final String description;

    MatchExceptionApplicationStatusEnum(String code, String description) {
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

    public static MatchExceptionApplicationStatusEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ServiceException("异常申请状态不能为空");
        }
        for (MatchExceptionApplicationStatusEnum value : values()) {
            if (value.code.equalsIgnoreCase(code.trim())) {
                return value;
            }
        }
        throw new ServiceException("不支持的异常申请状态");
    }
}
