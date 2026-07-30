package com.zhs.basket.mapper;

import org.apache.ibatis.annotations.Param;

public interface BasketTournamentSeasonPlatformMapper {

    int upsertOpenPlatform(@Param("seasonId") Long seasonId, @Param("platformCode") String platformCode);
}
