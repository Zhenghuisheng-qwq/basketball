package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

/**
 * basket_partner.partner_status
 */
public enum PartnerStatusEnum {
    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布"),
    OFFLINE(2, "已下线");

    private final int code;
    private final String description;

    PartnerStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public boolean canEdit() { return this == DRAFT || this == OFFLINE; }
    public boolean canPublish() { return this == DRAFT || this == OFFLINE; }
    public boolean canOffline() { return this == PUBLISHED; }
    public boolean isPublic() { return this == PUBLISHED; }

    public static PartnerStatusEnum fromCode(Integer code) {
        if (code == null) throw new ServiceException("未知合作伙伴状态");
        for (PartnerStatusEnum v : values()) {
            if (v.code == code) return v;
        }
        throw new ServiceException("未知合作伙伴状态");
    }
}
