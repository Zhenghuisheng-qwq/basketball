package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

import java.math.BigDecimal;

public enum PartnerSupportTypeEnum {
    CASH("CASH", "现金赞助"),
    GOODS("GOODS", "物资支持"),
    TECHNOLOGY("TECHNOLOGY", "技术支持"),
    VENUE("VENUE", "场地支持"),
    MEDIA("MEDIA", "媒体支持"),
    SERVICE("SERVICE", "服务支持"),
    OTHER("OTHER", "其他支持");

    private final String code;
    private final String description;

    PartnerSupportTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PartnerSupportTypeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ServiceException("支持方式不能为空");
        }
        for (PartnerSupportTypeEnum item : values()) {
            if (item.code.equalsIgnoreCase(code.trim())) {
                return item;
            }
        }
        throw new ServiceException("未知支持方式");
    }

    public static void validateAmount(String code, BigDecimal amount) {
        PartnerSupportTypeEnum type = fromCode(code);
        if (type == CASH) {
            if (amount == null) {
                throw new ServiceException("现金赞助金额不能为空");
            }
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ServiceException("现金赞助金额必须大于 0");
            }
        }
    }
}
