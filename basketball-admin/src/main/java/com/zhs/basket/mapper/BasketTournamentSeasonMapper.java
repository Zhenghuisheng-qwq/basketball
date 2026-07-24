package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTournamentSeason;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface BasketTournamentSeasonMapper {
    List<BasketTournamentSeason> selectByTournamentId(Long tournamentId);
    BasketTournamentSeason selectById(Long id);
    BasketTournamentSeason selectByIdForUpdate(Long id);
    int insert(BasketTournamentSeason season);
    int updateById(BasketTournamentSeason season);
    int softDelete(Long id);
    int updateStatusIfMatch(@Param("id") Long id, @Param("fromStatuses") List<Integer> fromStatuses,
            @Param("toStatus") Integer toStatus);
}
