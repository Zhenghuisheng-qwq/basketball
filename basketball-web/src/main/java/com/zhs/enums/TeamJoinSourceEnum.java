package com.zhs.enums;

import com.zhs.exception.ServiceException;
import org.springframework.util.StringUtils;

public enum TeamJoinSourceEnum {

    CREATE("CREATE", "创建"),
    INVITE("INVITE", "邀请"),
    APPLY("APPLY", "申请"),
    PERSONAL_DRAW("PERSONAL_DRAW", "个人抽签成队");

    private final String code;
    private final String description;

    TeamJoinSourceEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static TeamJoinSourceEnum fromCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new ServiceException("未知类型");
        }
        for (TeamJoinSourceEnum value : values()) {
            if (value.code.equalsIgnoreCase(code.trim())) {
                return value;
            }
        }
        throw new ServiceException("未知类型");
    }
}
