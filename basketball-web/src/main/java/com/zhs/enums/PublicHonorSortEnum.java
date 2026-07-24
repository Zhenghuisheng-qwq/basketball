package com.zhs.enums;

import com.zhs.exception.ServiceException;

/**
 * 公开荣誉列表排序白名单。
 */
public enum PublicHonorSortEnum {
    AWARD_DATE("awardDate"),
    DISPLAY_ORDER("displayOrder");

    private final String code;

    PublicHonorSortEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static PublicHonorSortEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return DISPLAY_ORDER;
        }
        String normalized = code.trim();
        for (PublicHonorSortEnum v : values()) {
            if (v.name().equalsIgnoreCase(normalized) || v.code.equalsIgnoreCase(normalized)) {
                return v;
            }
        }
        throw new ServiceException("非法荣誉排序字段: " + code);
    }
}
