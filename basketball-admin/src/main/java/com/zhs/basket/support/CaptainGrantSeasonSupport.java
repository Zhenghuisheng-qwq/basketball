package com.zhs.basket.support;

import com.zhs.basket.domain.BasketTournament;
import com.zhs.basket.domain.BasketTournamentSeason;
import com.zhs.basket.enums.SeasonStatusEnum;
import com.zhs.common.exception.ServiceException;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * 队长授权与赛事赛季（运营单元）的关联规则。
 */
public final class CaptainGrantSeasonSupport {

    private static final Set<Integer> GRANTABLE_SEASON_STATUSES = new HashSet<>(Arrays.asList(
            SeasonStatusEnum.PREPARE.getCode(),
            SeasonStatusEnum.REGISTRATION_OPEN.getCode(),
            SeasonStatusEnum.REGISTRATION_CLOSED.getCode(),
            SeasonStatusEnum.IN_PROGRESS.getCode()
    ));

    private static final Set<Integer> ENDED_SEASON_STATUSES = new HashSet<>(Arrays.asList(
            SeasonStatusEnum.FINISHED.getCode(),
            SeasonStatusEnum.CLOSED.getCode(),
            SeasonStatusEnum.CANCELLED.getCode()
    ));

    private CaptainGrantSeasonSupport() {
    }

    public static boolean isGrantableSeasonStatus(Integer seasonStatus) {
        return seasonStatus != null && GRANTABLE_SEASON_STATUSES.contains(seasonStatus);
    }

    public static boolean isSeasonEnded(Integer seasonStatus) {
        return seasonStatus != null && ENDED_SEASON_STATUSES.contains(seasonStatus);
    }

    public static void assertGrantableSeason(BasketTournamentSeason season) {
        if (season == null) {
            throw new ServiceException("赛事不存在");
        }
        if (isSeasonEnded(season.getSeasonStatus())) {
            throw new ServiceException("当前赛事已结束，不能授予队长权限");
        }
        if (!isGrantableSeasonStatus(season.getSeasonStatus())) {
            throw new ServiceException("当前赛事状态不可授予队长权限");
        }
    }

    public static Date resolveExpireTime(BasketTournamentSeason season, BasketTournament tournament) {
        if (season != null && season.getSeasonEndTime() != null) {
            return season.getSeasonEndTime();
        }
        if (tournament != null && tournament.getCompetitionEndTime() != null) {
            return tournament.getCompetitionEndTime();
        }
        return null;
    }

    public static Date requireExpireTime(BasketTournamentSeason season, BasketTournament tournament) {
        Date expireTime = resolveExpireTime(season, tournament);
        if (expireTime == null) {
            throw new ServiceException("当前赛事未配置结束时间，无法授予队长权限");
        }
        return expireTime;
    }
}
