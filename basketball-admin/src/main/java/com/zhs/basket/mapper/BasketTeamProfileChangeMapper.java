package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTeamProfileChange;
import com.zhs.basket.query.TeamProfileChangeQuery;
import com.zhs.basket.vo.TeamProfileChangeDetailVo;
import com.zhs.basket.vo.TeamProfileChangeListVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface BasketTeamProfileChangeMapper {

    List<TeamProfileChangeListVo> selectList(TeamProfileChangeQuery query);

    TeamProfileChangeDetailVo selectDetailVo(@Param("id") Long id);

    BasketTeamProfileChange selectById(@Param("id") Long id);

    int updateStatusIfMatch(@Param("id") Long id,
                            @Param("fromStatuses") List<Integer> fromStatuses,
                            @Param("toStatus") Integer toStatus,
                            @Param("auditUserId") Long auditUserId,
                            @Param("auditRemark") String auditRemark,
                            @Param("auditTime") Date auditTime,
                            @Param("activeFlag") Integer activeFlag);

    int countActiveByTeamIdExclude(@Param("teamId") Long teamId, @Param("excludeId") Long excludeId);
}
