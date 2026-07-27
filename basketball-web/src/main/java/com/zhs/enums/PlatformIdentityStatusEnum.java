package com.zhs.enums;

/**
 * 平台游戏身份展示状态（API 层）
 */
public enum PlatformIdentityStatusEnum {

    UNBOUND("UNBOUND", "未绑定"),
    PENDING("PENDING", "审核中"),
    ACTIVE("ACTIVE", "已生效"),
    REJECTED("REJECTED", "已驳回");

    private final String code;
    private final String label;

    PlatformIdentityStatusEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
