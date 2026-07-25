package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

/**
 * 赛事报名窗口状态（展示辅助字段，非赛事主状态）
 */
public enum TournamentRegistrationWindowStatusEnum {

    UNCONFIGURED("UNCONFIGURED", "未配置"),
    NOT_STARTED("NOT_STARTED", "未开始"),
    OPEN("OPEN", "报名中"),
    CLOSED("CLOSED", "已截止");

    private final String code;
    private final String description;

    TournamentRegistrationWindowStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static TournamentRegistrationWindowStatusEnum fromCode(String code) {
        if (code == null) {
            throw new ServiceException("未知报名窗口状态");
        }
        for (TournamentRegistrationWindowStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new ServiceException("未知报名窗口状态");
    }
}
