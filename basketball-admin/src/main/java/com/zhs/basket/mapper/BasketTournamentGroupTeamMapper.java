package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTournamentGroupTeam;
import com.zhs.basket.vo.AvailableTeamVo;
import com.zhs.basket.vo.GroupTeamVo;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface BasketTournamentGroupTeamMapper {
    List<GroupTeamVo> selectByGroupId(Long groupId);
    List<AvailableTeamVo> selectAvailableTeamsByStageId(@Param("seasonId") Long seasonId, @Param("stageId") Long stageId);
    int countByStageIdAndTeamId(@Param("stageId") Long stageId, @Param("teamId") Long teamId,
            @Param("excludeGroupId") Long excludeGroupId);
    int countByGroupIdAndTeamId(@Param("groupId") Long groupId, @Param("teamId") Long teamId);
    int softDeleteByGroupId(Long groupId);
    int batchInsert(@Param("list") List<BasketTournamentGroupTeam> list);
}
