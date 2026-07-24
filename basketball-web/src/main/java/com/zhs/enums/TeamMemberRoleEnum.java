package com.zhs.enums;

import com.zhs.exception.ServiceException;
import org.springframework.util.StringUtils;

public enum TeamMemberRoleEnum {

    CAPTAIN("CAPTAIN", "队长"),
    MEMBER("MEMBER", "队员");

    private final String code;
    private final String description;

    TeamMemberRoleEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static TeamMemberRoleEnum fromCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new ServiceException("未知类型");
        }
        for (TeamMemberRoleEnum value : values()) {
            if (value.code.equalsIgnoreCase(code.trim())) {
                return value;
            }
        }
        throw new ServiceException("未知类型");
    }
}
