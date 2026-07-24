package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTeamSeasonStat;
import com.zhs.basket.query.TeamSeasonStatQuery;
import com.zhs.basket.vo.TeamSeasonStatListVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface BasketTeamSeasonStatAdminMapper {
    List<TeamSeasonStatListVo> selectList(TeamSeasonStatQuery query);

    int softDeleteByScope(@Param("seasonId") Long seasonId, @Param("stageId") Long stageId);

    int batchInsert(@Param("list") List<BasketTeamSeasonStat> list);

    int countBySeason(@Param("seasonId") Long seasonId);

    Date selectMaxCalculatedTime(@Param("seasonId") Long seasonId);

    List<TeamSeasonStatListVo> selectForRanking(@Param("seasonId") Long seasonId, @Param("stageId") Long stageId);
}
