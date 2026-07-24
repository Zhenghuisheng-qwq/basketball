package com.zhs.basket.enums;

/**
 * 赛季最终名次业务状态（无独立名次表，由赛季冠亚军字段 + 荣誉发布状态推导）。
 */
public enum SeasonResultStatusEnum {
    DRAFT("DRAFT", "未确认"),
    CONFIRMED("CONFIRMED", "已确认未发布"),
    PUBLISHED("PUBLISHED", "已发布"),
    REVOKED("REVOKED", "已撤销");

    private final String code;
    private final String description;

    SeasonResultStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }
}
