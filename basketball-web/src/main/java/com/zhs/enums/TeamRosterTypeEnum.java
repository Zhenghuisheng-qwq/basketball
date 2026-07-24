package com.zhs.enums;

import com.zhs.exception.ServiceException;
import org.springframework.util.StringUtils;

public enum TeamRosterTypeEnum {

    OFFICIAL("OFFICIAL", "正式"),
    TRIAL("TRIAL", "试训");

    private final String code;
    private final String description;

    TeamRosterTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static TeamRosterTypeEnum fromCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new ServiceException("未知类型");
        }
        for (TeamRosterTypeEnum value : values()) {
            if (value.code.equalsIgnoreCase(code.trim())) {
                return value;
            }
        }
        throw new ServiceException("未知类型");
    }
}
