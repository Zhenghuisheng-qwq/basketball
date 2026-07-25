package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTeamMember;
import com.zhs.basket.vo.AccountPlayerTeamSummaryVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketTeamMemberMapper {
    int countActiveCaptainTeams(@Param("playerId") Long playerId);
    int countCurrentMembers(@Param("teamId") Long teamId);
    BasketTeamMember selectCaptainMember(@Param("teamId") Long teamId);
    List<AccountPlayerTeamSummaryVo> selectTeamSummaryByMemberId(@Param("memberId") Long memberId);
}
