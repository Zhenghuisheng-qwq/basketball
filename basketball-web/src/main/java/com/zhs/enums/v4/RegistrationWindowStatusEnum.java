package com.zhs.enums.v4;

/** 报名时间窗口（与 JDBC serverTimezone=GMT+8 对齐）。 */
public enum RegistrationWindowStatusEnum {
    NOT_STARTED("NOT_STARTED", "报名未开始"),
    OPEN("OPEN", "报名中"),
    CLOSED("CLOSED", "报名已截止");

    private final String code;
    private final String label;

    RegistrationWindowStatusEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }
}
