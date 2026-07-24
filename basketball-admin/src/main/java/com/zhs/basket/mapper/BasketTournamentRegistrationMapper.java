package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTournamentRegistration;
import com.zhs.basket.query.RegistrationAuditQuery;
import com.zhs.basket.vo.AvailableTeamVo;
import org.apache.ibatis.annotations.Param;
import java.util.Date;
import java.util.List;

public interface BasketTournamentRegistrationMapper {
    List<BasketTournamentRegistration> selectAuditList(RegistrationAuditQuery query);
    BasketTournamentRegistration selectById(Long id);
    int updateStatusIfMatch(@Param("id") Long id, @Param("fromStatuses") List<Integer> fromStatuses,
            @Param("toStatus") Integer toStatus, @Param("currentAuditorId") Long currentAuditorId,
            @Param("remark") String remark, @Param("auditTime") Date auditTime, @Param("auditorId") Long auditorId);
    int countApprovedBySeasonId(@Param("seasonId") Long seasonId);
    List<AvailableTeamVo> selectApprovedTeamsBySeasonId(@Param("seasonId") Long seasonId);
    BasketTournamentRegistration selectApprovedBySeasonAndTeam(@Param("seasonId") Long seasonId,
            @Param("teamId") Long teamId);
}
