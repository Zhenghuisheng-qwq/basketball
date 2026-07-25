package com.zhs.enums;

import com.zhs.exception.ServiceException;

/**
 * 平台游戏 ID 变更申请状态（basket_player_platform_change_request.request_status）
 */
public enum PlatformChangeStatusEnum {

    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已驳回"),
    WITHDRAWN(3, "已撤回");

    private final int code;
    private final String label;

    PlatformChangeStatusEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public boolean isPending() {
        return this == PENDING;
    }

    public static PlatformChangeStatusEnum fromCode(Integer code) {
        if (code == null) {
            return PENDING;
        }
        for (PlatformChangeStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new ServiceException("未知变更申请状态");
    }
}
