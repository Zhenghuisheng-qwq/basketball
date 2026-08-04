package com.zhs.enums.v4;

/**
 * CNPA 赛事域 V4 枚举：PlatformCodeEnum
 * 新业务写入仅允许 PC / PS_XBOX；PS / XBOX 仅用于历史数据兼容。
 */
public enum PlatformCodeEnum {
    PC("PC", "PC", false),
    PS_XBOX("PS_XBOX", "PS/Xbox", false),
    PS("PS", "PS（历史）", true),
    XBOX("XBOX", "Xbox（历史）", true);

    private final String code;
    private final String description;
    private final boolean legacy;

    PlatformCodeEnum(String code, String description, boolean legacy) {
        this.code = code;
        this.description = description;
        this.legacy = legacy;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public boolean isLegacy() {
        return legacy;
    }

    public static PlatformCodeEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (PlatformCodeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知PlatformCodeEnum: " + code);
    }

    public static boolean isWritableCode(String code) {
        if (code == null || code.isEmpty()) {
            return false;
        }
        PlatformCodeEnum platform = fromCode(code);
        return !platform.isLegacy();
    }
}
