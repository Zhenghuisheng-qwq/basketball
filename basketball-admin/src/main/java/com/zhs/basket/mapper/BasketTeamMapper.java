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

    int updateProfileFromChange(@Param("id") Long id,
                                @Param("teamName") String teamName,
                                @Param("shortName") String shortName,
                                @Param("logoFileId") Long logoFileId,
                                @Param("description") String description,
                                @Param("publicStatus") Integer publicStatus);

    int countByTeamNameExclude(@Param("divisionId") Long divisionId,
                               @Param("teamName") String teamName,
                               @Param("excludeTeamId") Long excludeTeamId);

    List<Long> selectIdsByDivisionId(@Param("divisionId") Long divisionId);

    int archiveActiveByDivisionId(@Param("divisionId") Long divisionId,
                                  @Param("fromStatus") Integer fromStatus,
                                  @Param("toStatus") Integer toStatus,
                                  @Param("finishTime") java.util.Date finishTime);
}
