package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

/**
 * basket_honor_record.display_status
 */
public enum HonorDisplayStatusEnum {
    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布"),
    OFFLINE(2, "已下线"),
    REVOKED(3, "已撤销");

    private final int code;
    private final String description;

    HonorDisplayStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public boolean canEdit() { return this == DRAFT || this == OFFLINE; }
    public boolean canPublish() { return this == DRAFT || this == OFFLINE; }
    public boolean canRevoke() { return this == PUBLISHED || this == DRAFT; }
    public boolean canOffline() { return this == PUBLISHED; }
    public boolean isPublic() { return this == PUBLISHED; }

    public static HonorDisplayStatusEnum fromCode(Integer code) {
        if (code == null) throw new ServiceException("未知荣誉展示状态");
        for (HonorDisplayStatusEnum v : values()) {
            if (v.code == code) return v;
        }
        throw new ServiceException("未知荣誉展示状态");
    }
}
