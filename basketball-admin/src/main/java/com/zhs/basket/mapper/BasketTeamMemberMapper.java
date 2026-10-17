package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTeamMember;
import com.zhs.basket.vo.AccountPlayerTeamSummaryVo;
import com.zhs.basket.vo.TeamAuditMemberVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketTeamMemberMapper {
    int countActiveCaptainTeams(@Param("playerId") Long playerId);
    int countCurrentMembers(@Param("teamId") Long teamId);
    BasketTeamMember selectCaptainMember(@Param("teamId") Long teamId);
    List<AccountPlayerTeamSummaryVo> selectTeamSummaryByMemberId(@Param("memberId") Long memberId);
    List<TeamAuditMemberVo> selectCurrentMembersForAudit(@Param("teamId") Long teamId);

    int countActiveMemberByTeamAndPlayer(@Param("teamId") Long teamId, @Param("playerId") Long playerId);

    BasketTeamMember selectCurrentByTeamAndPlayer(@Param("teamId") Long teamId, @Param("playerId") Long playerId);

    int insertActive(BasketTeamMember member);

    int endCurrent(@Param("id") Long id,
                   @Param("status") Integer status,
                   @Param("leaveTime") java.util.Date leaveTime,
                   @Param("leaveReason") String leaveReason);

    int countActiveMembershipByPlayer(@Param("playerId") Long playerId);

    int historicalizeActiveByDivisionId(@Param("divisionId") Long divisionId,
                                        @Param("fromStatus") Integer fromStatus,
                                        @Param("toStatus") Integer toStatus,
                                        @Param("leaveTime") java.util.Date leaveTime,
                                        @Param("leaveReason") String leaveReason);
}
