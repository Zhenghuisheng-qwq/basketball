package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

/**
 * basket_announcement.announcement_status
 */
public enum AnnouncementStatusEnum {
    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布"),
    OFFLINE(2, "已下线");

    private final int code;
    private final String description;

    AnnouncementStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public boolean canEdit() { return this == DRAFT || this == OFFLINE; }
    public boolean canPublish() { return this == DRAFT || this == OFFLINE; }
    public boolean canOffline() { return this == PUBLISHED; }
    public boolean canTop() { return this == PUBLISHED; }
    public boolean canDelete() { return this == DRAFT || this == OFFLINE; }
    public boolean isPublic() { return this == PUBLISHED; }

    public static AnnouncementStatusEnum fromCode(Integer code) {
        if (code == null) throw new ServiceException("未知公告状态");
        for (AnnouncementStatusEnum v : values()) {
            if (v.code == code) return v;
        }
        throw new ServiceException("未知公告状态");
    }
}
