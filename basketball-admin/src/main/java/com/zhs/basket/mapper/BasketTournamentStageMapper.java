package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTournamentStage;
import com.zhs.basket.query.StageQuery;
import com.zhs.basket.vo.StageListVo;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface BasketTournamentStageMapper {
    List<StageListVo> selectList(StageQuery query);
    BasketTournamentStage selectById(Long id);
    int countBySeasonIdAndStageName(@Param("seasonId") Long seasonId, @Param("stageName") String stageName,
            @Param("excludeId") Long excludeId);
    int insert(BasketTournamentStage stage);
    int updateById(BasketTournamentStage stage);
    int softDelete(Long id);
    int updateStatusIfMatch(@Param("id") Long id, @Param("fromStatuses") List<Integer> fromStatuses,
            @Param("toStatus") Integer toStatus);
}
