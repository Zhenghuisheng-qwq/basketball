package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTeamMembershipChange;
import com.zhs.basket.query.TeamMembershipChangeQuery;
import com.zhs.basket.vo.TeamMembershipChangeDetailVo;
import com.zhs.basket.vo.TeamMembershipChangeListVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface BasketTeamMembershipChangeMapper {
    List<TeamMembershipChangeListVo> selectList(TeamMembershipChangeQuery query);

    TeamMembershipChangeDetailVo selectDetail(@Param("id") Long id);

    BasketTeamMembershipChange selectById(@Param("id") Long id);

    BasketTeamMembershipChange selectByIdForUpdate(@Param("id") Long id);

    int countPendingAdmin();

    int countPendingByTeamAndType(@Param("teamId") Long teamId, @Param("changeType") String changeType);

    int markApproved(@Param("id") Long id,
                     @Param("auditUserId") Long auditUserId,
                     @Param("auditUserName") String auditUserName,
                     @Param("auditRemark") String auditRemark,
                     @Param("auditTime") Date auditTime,
                     @Param("effectStage") String effectStage,
                     @Param("impactSummary") String impactSummary);

    int markRejected(@Param("id") Long id,
                     @Param("auditUserId") Long auditUserId,
                     @Param("auditUserName") String auditUserName,
                     @Param("auditRemark") String auditRemark,
                     @Param("auditTime") Date auditTime);

    int updateInvitationStatus(@Param("id") Long id, @Param("status") Integer status);
}
