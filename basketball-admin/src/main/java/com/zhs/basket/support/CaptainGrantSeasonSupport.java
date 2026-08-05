package com.zhs.basket.support;

import com.zhs.basket.domain.v4.BasketSeason;
import com.zhs.basket.domain.v4.BasketSeries;
import com.zhs.basket.enums.v4.SeasonStatusEnum;
import com.zhs.common.exception.ServiceException;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * 队长授权与 V4 赛季的关联规则。
 */
public final class CaptainGrantSeasonSupport {

    private static final Set<String> GRANTABLE_SEASON_STATUSES = new HashSet<>(Arrays.asList(
            SeasonStatusEnum.NOT_STARTED.getCode(),
            SeasonStatusEnum.REGISTRATION_OPEN.getCode(),
            SeasonStatusEnum.REGISTRATION_CLOSED.getCode(),
            SeasonStatusEnum.PLAYING.getCode()
    ));

    private static final Set<String> ENDED_SEASON_STATUSES = new HashSet<>(Arrays.asList(
            SeasonStatusEnum.FINISHED.getCode(),
            SeasonStatusEnum.ARCHIVED.getCode()
    ));

    private CaptainGrantSeasonSupport() {
    }

    public static boolean isGrantableSeasonStatus(String seasonStatus) {
        return seasonStatus != null && GRANTABLE_SEASON_STATUSES.contains(seasonStatus);
    }

    public static boolean isSeasonEnded(String seasonStatus) {
        return seasonStatus != null && ENDED_SEASON_STATUSES.contains(seasonStatus);
    }

    public static void assertGrantableSeason(BasketSeason season) {
        if (season == null) {
            throw new ServiceException("赛季不存在");
        }
        if (isSeasonEnded(season.getSeasonStatus())) {
            throw new ServiceException("当前赛季已结束，不能授予队长权限");
        }
        if (!isGrantableSeasonStatus(season.getSeasonStatus())) {
            throw new ServiceException("当前赛季状态不可授予队长权限");
        }
    }

    public static Date resolveExpireTime(BasketSeason season, BasketSeries series) {
        if (season != null && season.getSeasonEndTime() != null) {
            return season.getSeasonEndTime();
        }
        return null;
    }

    public static Date requireExpireTime(BasketSeason season, BasketSeries series) {
        Date expireTime = resolveExpireTime(season, series);
        if (expireTime == null) {
            throw new ServiceException("当前赛季未配置结束时间，无法授予队长权限");
        }
        return expireTime;
    }
}
