package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum MatchOfficialResultTypeEnum {
    NORMAL("NORMAL", "正常"),
    FORFEIT("FORFEIT", "弃赛"),
    ADMIN_DECISION("ADMIN_DECISION", "管理员裁定"),
    REPLAY_RESULT("REPLAY_RESULT", "重赛结果"),
    VOID("VOID", "作废");

    private final String code;
    private final String description;

    MatchOfficialResultTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static MatchOfficialResultTypeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return NORMAL;
        }
        for (MatchOfficialResultTypeEnum v : values()) {
            if (v.code.equalsIgnoreCase(code.trim())) {
                return v;
            }
        }
        throw new ServiceException("未知正式结果类型");
    }
}
