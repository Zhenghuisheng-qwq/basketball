package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTournamentGroup;
import com.zhs.basket.vo.GroupListVo;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface BasketTournamentGroupMapper {
    List<GroupListVo> selectByStageId(Long stageId);
    BasketTournamentGroup selectById(Long id);
    int countByStageIdAndGroupName(@Param("stageId") Long stageId, @Param("groupName") String groupName,
            @Param("excludeId") Long excludeId);
    int insert(BasketTournamentGroup group);
    int updateById(BasketTournamentGroup group);
    int softDelete(Long id);
}
