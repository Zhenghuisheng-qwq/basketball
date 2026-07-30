package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum ScheduleBatchPublishStatusEnum {
    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布"),
    UNPUBLISHED(2, "已下架");

    private final int code;
    private final String description;

    ScheduleBatchPublishStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static ScheduleBatchPublishStatusEnum fromCode(Integer code) {
        if (code == null) {
            throw new ServiceException("批次发布状态不能为空");
        }
        for (ScheduleBatchPublishStatusEnum v : values()) {
            if (v.code == code) {
                return v;
            }
        }
        throw new ServiceException("未知批次发布状态: " + code);
    }
}
