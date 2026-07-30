package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTournamentSeason;
import com.zhs.basket.dto.SeasonChampionDto;
import com.zhs.basket.resp.SeasonResultListResp;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketTournamentSeasonMapper {
    List<BasketTournamentSeason> selectByTournamentId(Long tournamentId);
    BasketTournamentSeason selectPrimaryByTournamentId(@Param("tournamentId") Long tournamentId);
    BasketTournamentSeason selectById(Long id);
    BasketTournamentSeason selectByIdForUpdate(Long id);
    int insert(BasketTournamentSeason season);
    int updateById(BasketTournamentSeason season);
    int softDelete(Long id);
    int updateStatusIfMatch(@Param("id") Long id, @Param("fromStatuses") List<Integer> fromStatuses,
            @Param("toStatus") Integer toStatus);

    List<SeasonResultListResp> selectSeasonResultList(@Param("tournamentId") Long tournamentId);

    SeasonChampionDto selectChampionFields(@Param("seasonId") Long seasonId);

    int updateChampionIfAbsent(@Param("id") Long id,
                               @Param("championTeamId") Long championTeamId,
                               @Param("runnerUpTeamId") Long runnerUpTeamId);

    int clearChampionIfMatch(@Param("id") Long id,
                             @Param("championTeamId") Long championTeamId,
                             @Param("runnerUpTeamId") Long runnerUpTeamId);

    /** 报名已截止且未删除的赛季 ID 列表（供自动排赛扫描） */
    List<Long> selectRegistrationClosedSeasonIds(@Param("now") java.util.Date now);
}
