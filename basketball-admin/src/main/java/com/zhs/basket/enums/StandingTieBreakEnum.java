package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

/**
 * 积分榜同分排序维度（顺序来自赛事规则配置）。
 */
public enum StandingTieBreakEnum {
    STANDING_POINTS,
    HEAD_TO_HEAD_POINTS,
    HEAD_TO_HEAD_DIFF,
    POINT_DIFF,
    POINTS_FOR,
    POINTS_AGAINST_ASC,
    WINS,
    TEAM_ID;

    public static StandingTieBreakEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ServiceException("未知同分规则: " + code);
        }
        try {
            return StandingTieBreakEnum.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ServiceException("未知同分规则: " + code);
        }
    }
}
