package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTournamentRosterChangeRecord;
import com.zhs.basket.vo.SeasonRosterChangeRecordVo;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface BasketTournamentRosterChangeRecordMapper {
    int insert(BasketTournamentRosterChangeRecord record);
    List<SeasonRosterChangeRecordVo> selectByPlayerAndSeason(@Param("seasonId") Long seasonId,
            @Param("playerId") Long playerId);
    List<SeasonRosterChangeRecordVo> selectByEventTeam(@Param("eventTeamId") Long eventTeamId);
}
