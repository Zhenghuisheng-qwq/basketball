package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketMatchRoster;
import com.zhs.basket.vo.MatchLineupPlayerVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketMatchRosterMapper {
    List<MatchLineupPlayerVo> selectVoByMatchAndTeam(@Param("matchId") Long matchId, @Param("teamId") Long teamId);
    int countStartersByMatchAndTeam(@Param("matchId") Long matchId, @Param("teamId") Long teamId);
    int countCaptainsByMatchAndTeam(@Param("matchId") Long matchId, @Param("teamId") Long teamId);
    int softDeleteByMatchAndTeam(@Param("matchId") Long matchId, @Param("teamId") Long teamId);
    int batchInsert(@Param("list") List<BasketMatchRoster> list);
    int lockByMatchAndTeam(@Param("matchId") Long matchId, @Param("teamId") Long teamId);
    int countLockedByMatch(@Param("matchId") Long matchId);
    List<Long> selectMemberIdsByMatchAndTeam(@Param("matchId") Long matchId, @Param("teamId") Long teamId);
}
