package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum MatchPublishStatusEnum {
    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布"),
    UNPUBLISHED(2, "已下架");

    private final int code;
    private final String description;

    MatchPublishStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public boolean isPublicVisible() {
        return this == PUBLISHED;
    }

    public static MatchPublishStatusEnum fromCode(Integer code) {
        if (code == null) {
            throw new ServiceException("发布状态不能为空");
        }
        for (MatchPublishStatusEnum v : values()) {
            if (v.code == code) {
                return v;
            }
        }
        throw new ServiceException("未知发布状态: " + code);
    }
}
