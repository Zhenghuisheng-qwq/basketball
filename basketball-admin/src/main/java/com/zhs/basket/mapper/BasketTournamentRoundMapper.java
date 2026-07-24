package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTournamentRound;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface BasketTournamentRoundMapper {
    List<BasketTournamentRound> selectByStageId(Long stageId);
    BasketTournamentRound selectById(Long id);
    int countByStageIdAndRoundName(@Param("stageId") Long stageId, @Param("roundName") String roundName,
            @Param("excludeId") Long excludeId);
    int insert(BasketTournamentRound round);
    int updateById(BasketTournamentRound round);
    int softDelete(Long id);
    int updateStatusIfMatch(@Param("id") Long id, @Param("fromStatuses") List<Integer> fromStatuses,
            @Param("toStatus") Integer toStatus);
}
