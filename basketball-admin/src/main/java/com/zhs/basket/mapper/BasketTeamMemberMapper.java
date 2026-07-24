package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTeamMember;
import org.apache.ibatis.annotations.Param;

public interface BasketTeamMemberMapper {
    int countActiveCaptainTeams(@Param("playerId") Long playerId);
    int countCurrentMembers(@Param("teamId") Long teamId);
    BasketTeamMember selectCaptainMember(@Param("teamId") Long teamId);
}
