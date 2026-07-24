package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketStageStanding;
import com.zhs.basket.query.StandingQuery;
import com.zhs.basket.vo.StandingListVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface BasketStageStandingAdminMapper {
    List<StandingListVo> selectList(StandingQuery query);

    int softDeleteByScope(@Param("seasonId") Long seasonId,
                          @Param("stageId") Long stageId,
                          @Param("groupId") Long groupId);

    int batchInsert(@Param("list") List<BasketStageStanding> list);

    int countBySeason(@Param("seasonId") Long seasonId);

    Date selectMaxSnapshotTime(@Param("seasonId") Long seasonId);

    List<Long> selectGroupIdsByStage(@Param("stageId") Long stageId);

    List<Long> selectTeamIdsByGroup(@Param("groupId") Long groupId);

    List<Long> selectStageIdsBySeason(@Param("seasonId") Long seasonId);
}
