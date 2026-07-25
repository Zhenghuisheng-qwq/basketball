package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTeamSeasonProfile;
import com.zhs.basket.query.EventTeamAdminQuery;
import com.zhs.basket.vo.EventTeamDetailVo;
import com.zhs.basket.vo.EventTeamListVo;
import org.apache.ibatis.annotations.Param;
import java.util.Date;
import java.util.List;

public interface BasketTeamSeasonProfileMapper {
    int insert(BasketTeamSeasonProfile profile);
    BasketTeamSeasonProfile selectById(Long id);
    BasketTeamSeasonProfile selectByRegistrationId(@Param("registrationId") Long registrationId);
    BasketTeamSeasonProfile selectByTeamIdAndSeasonId(@Param("teamId") Long teamId, @Param("seasonId") Long seasonId);
    List<EventTeamListVo> selectAdminList(EventTeamAdminQuery query);
    EventTeamDetailVo selectDetailVo(@Param("id") Long id);
    Integer selectMaxEventTeamNoSequence(@Param("seasonId") Long seasonId);
    int countBySeasonAndEventTeamNo(@Param("seasonId") Long seasonId, @Param("eventTeamNo") String eventTeamNo);
    int updateRosterLockStatus(@Param("id") Long id, @Param("rosterLockStatus") Integer rosterLockStatus,
            @Param("rosterLockedTime") Date rosterLockedTime);
    int updateParticipationStatus(@Param("id") Long id, @Param("participationStatus") Integer participationStatus);
    int updateCaptainPlayerId(@Param("id") Long id, @Param("captainPlayerId") Long captainPlayerId);
}
