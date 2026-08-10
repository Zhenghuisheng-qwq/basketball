package com.zhs.basket.mapper.v4;

import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Division Closing 批量收尾（Invitation / Application / Division CAS）。
 */
public interface BasketTeamLifecycleClosingMapper {

    int casFinishDivision(@Param("divisionId") Long divisionId,
                          @Param("fromStatuses") Collection<String> fromStatuses,
                          @Param("finishTime") Date finishTime);

    int expirePendingInvitationsByTeamIds(@Param("teamIds") List<Long> teamIds,
                                          @Param("fromStatus") int fromStatus,
                                          @Param("toStatus") int toStatus,
                                          @Param("finishTime") Date finishTime);

    int cancelPendingApplicationsByTeamIds(@Param("teamIds") List<Long> teamIds,
                                           @Param("fromStatus") int fromStatus,
                                           @Param("toStatus") int toStatus,
                                           @Param("finishTime") Date finishTime);
}
