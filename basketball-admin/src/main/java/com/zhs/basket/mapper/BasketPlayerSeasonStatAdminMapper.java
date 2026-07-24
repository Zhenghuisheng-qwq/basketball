package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketPlayerSeasonStat;
import com.zhs.basket.query.PlayerSeasonStatQuery;
import com.zhs.basket.vo.PlayerSeasonStatListVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface BasketPlayerSeasonStatAdminMapper {
    List<PlayerSeasonStatListVo> selectList(PlayerSeasonStatQuery query);

    int softDeleteByScope(@Param("seasonId") Long seasonId, @Param("stageId") Long stageId);

    int batchInsert(@Param("list") List<BasketPlayerSeasonStat> list);

    int countBySeason(@Param("seasonId") Long seasonId);

    Date selectMaxCalculatedTime(@Param("seasonId") Long seasonId);

    List<PlayerSeasonStatListVo> selectForRanking(@Param("seasonId") Long seasonId, @Param("stageId") Long stageId);
}
