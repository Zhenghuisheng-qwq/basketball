package com.zhs.enums;

/**
 * 会员账号状态（对应 basket_member_account.account_status）
 */
public enum MemberAccountStatusEnum {

    DISABLED(0, "禁用"),
    NORMAL(1, "正常");

    private final int code;
    private final String label;

    MemberAccountStatusEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static boolean isNormal(Integer status) {
        return status != null && status == NORMAL.code;
    }
}
