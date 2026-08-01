package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTournamentSeasonRoster;
import com.zhs.basket.query.SeasonRosterAdminQuery;
import com.zhs.basket.vo.SeasonRosterListVo;
import org.apache.ibatis.annotations.Param;
import java.util.Date;
import java.util.List;

public interface BasketTournamentSeasonRosterMapper {
    int insert(BasketTournamentSeasonRoster roster);
    BasketTournamentSeasonRoster selectById(Long id);
    List<BasketTournamentSeasonRoster> selectByRegistrationId(@Param("registrationId") Long registrationId);
    List<BasketTournamentSeasonRoster> selectActiveByEventTeamId(@Param("eventTeamId") Long eventTeamId);
    List<SeasonRosterListVo> selectAdminList(SeasonRosterAdminQuery query);
    Integer selectMaxRosterNoSequence();
    int countByRosterNo(@Param("rosterNo") String rosterNo);
    int countActiveBySeasonAndPlayer(@Param("seasonId") Long seasonId, @Param("playerId") Long playerId);
    int invalidateActiveRoster(@Param("id") Long id, @Param("rosterStatus") Integer rosterStatus,
            @Param("invalidTime") Date invalidTime);
    int updateCaptainFlag(@Param("eventTeamId") Long eventTeamId, @Param("playerId") Long playerId,
            @Param("isCaptain") Integer isCaptain);
    int clearCaptainByEventTeam(@Param("eventTeamId") Long eventTeamId);
    int updateRoleFields(@Param("id") Long id, @Param("rosterRole") String rosterRole,
            @Param("isStarting") Integer isStarting, @Param("isSubstitute") Integer isSubstitute);
    int updateLockStatusByEventTeamId(@Param("eventTeamId") Long eventTeamId,
            @Param("lockStatus") Integer lockStatus, @Param("updateTime") Date updateTime);

    int countUnlockedActiveByEventTeamId(@Param("eventTeamId") Long eventTeamId);

    List<Long> selectActiveMemberIdsBySeasonId(@Param("seasonId") Long seasonId);
}
