package com.zhs.basket.enums.v4;

/**
 * 参赛组织方式：是否分组（赛制三维模型第一维）。
 */
public enum OrganizationModeEnum {
    GROUPED("GROUPED", "分组"),
    UNGROUPED("UNGROUPED", "不分组");

    private final String code;
    private final String description;

    OrganizationModeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static OrganizationModeEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (OrganizationModeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知OrganizationModeEnum: " + code);
    }
}
