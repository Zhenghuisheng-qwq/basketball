package com.zhs.enums;

import com.zhs.exception.ServiceException;

public enum MatchExceptionTypeEnum {

    FORFEIT("FORFEIT", "弃赛/缺席"),
    POSTPONE("POSTPONE", "延期"),
    CANCEL("CANCEL", "取消"),
    REMATCH("REMATCH", "重赛申请"),
    OTHER("OTHER", "其他异常");

    private final String code;
    private final String description;

    MatchExceptionTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static MatchExceptionTypeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ServiceException("异常类型不能为空");
        }
        for (MatchExceptionTypeEnum value : values()) {
            if (value.code.equalsIgnoreCase(code.trim())) {
                return value;
            }
        }
        throw new ServiceException("不支持的异常类型");
    }
}
