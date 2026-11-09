package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

/**
 * basket_home_banner.banner_status
 */
public enum BannerStatusEnum {
    DRAFT(0, "未上架"),
    PUBLISHED(1, "已上架"),
    OFFLINE(2, "已下架");

    private final int code;
    private final String description;

    BannerStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public boolean canEdit() { return this == DRAFT || this == OFFLINE; }
    public boolean canPublish() { return this == DRAFT || this == OFFLINE; }
    public boolean canOffline() { return this == PUBLISHED; }
    public boolean isPublic() { return this == PUBLISHED; }

    public static BannerStatusEnum fromCode(Integer code) {
        if (code == null) throw new ServiceException("未知横幅状态");
        for (BannerStatusEnum v : values()) {
            if (v.code == code) return v;
        }
        throw new ServiceException("未知横幅状态");
    }
}
