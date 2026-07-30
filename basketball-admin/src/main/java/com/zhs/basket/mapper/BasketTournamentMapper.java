package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTournament;
import com.zhs.basket.query.TournamentQuery;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface BasketTournamentMapper {
    List<BasketTournament> selectList(TournamentQuery query);
    BasketTournament selectById(Long id);
    int insert(BasketTournament tournament);
    int updateById(BasketTournament tournament);
    int softDelete(Long id);
    int updateStatusIfMatch(@Param("id") Long id, @Param("fromStatuses") List<Integer> fromStatuses,
            @Param("toStatus") Integer toStatus);

    int countBySeasonAndName(@Param("competitionSeasonId") Long competitionSeasonId,
            @Param("tournamentName") String tournamentName,
            @Param("excludeId") Long excludeId);
}
