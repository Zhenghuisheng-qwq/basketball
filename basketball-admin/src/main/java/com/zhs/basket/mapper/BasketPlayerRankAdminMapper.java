package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketPlayerRank;
import com.zhs.basket.query.RankingQuery;
import com.zhs.basket.vo.PlayerRankListVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface BasketPlayerRankAdminMapper {
    List<PlayerRankListVo> selectLatestList(RankingQuery query);

    int softDeleteByScope(@Param("seasonId") Long seasonId,
                          @Param("stageId") Long stageId,
                          @Param("snapshotDate") Date snapshotDate);

    int batchInsert(@Param("list") List<BasketPlayerRank> list);

    int countBySeasonAndDate(@Param("seasonId") Long seasonId, @Param("snapshotDate") Date snapshotDate);

    Integer selectPreviousRank(@Param("seasonId") Long seasonId,
                               @Param("stageId") Long stageId,
                               @Param("rankType") String rankType,
                               @Param("playerId") Long playerId,
                               @Param("beforeDate") Date beforeDate);
}
