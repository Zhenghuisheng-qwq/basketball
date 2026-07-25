package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTeam;
import com.zhs.basket.query.TeamAuditQuery;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface BasketTeamMapper {
    List<BasketTeam> selectAuditList(TeamAuditQuery query);
    BasketTeam selectById(Long id);
    int updateAuditStatusIfMatch(@Param("id") Long id, @Param("fromStatuses") List<Integer> fromStatuses,
            @Param("toStatus") Integer toStatus, @Param("auditorId") Long auditorId);
    int updateMemberCount(@Param("id") Long id, @Param("memberCount") Integer memberCount);
    Integer selectMaxTeamNoSequence();
    int countByTeamNo(@Param("teamNo") String teamNo);
    int updateTeamNo(@Param("id") Long id, @Param("teamNo") String teamNo);
}
