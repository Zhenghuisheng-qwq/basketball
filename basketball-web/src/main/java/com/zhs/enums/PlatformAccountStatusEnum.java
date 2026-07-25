package com.zhs.enums;

import com.zhs.exception.ServiceException;

/**
 * 平台游戏 ID 账号状态（basket_player_platform_account.account_status）
 */
public enum PlatformAccountStatusEnum {

    PENDING(0, "待审核"),
    ACTIVE(1, "已生效"),
    INVALID(2, "已失效"),
    REJECTED(3, "已驳回");

    private final int code;
    private final String label;

    PlatformAccountStatusEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static PlatformAccountStatusEnum fromCode(Integer code) {
        if (code == null) {
            return PENDING;
        }
        for (PlatformAccountStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new ServiceException("未知平台账号状态");
    }
}
