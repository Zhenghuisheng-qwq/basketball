package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketMatchDataDispute;
import com.zhs.basket.query.MatchDisputeQuery;
import com.zhs.basket.vo.MatchDisputeListVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketMatchDataDisputeMapper {
    int insert(BasketMatchDataDispute row);

    int countPendingByMatchAndType(@Param("matchId") Long matchId, @Param("disputeType") String disputeType);

    List<MatchDisputeListVo> selectList(MatchDisputeQuery query);

    BasketMatchDataDispute selectById(Long id);

    int updateHandle(@Param("id") Long id, @Param("fromStatus") String fromStatus,
            @Param("toStatus") String toStatus, @Param("handlerId") Long handlerId,
            @Param("handleResult") String handleResult, @Param("handleRemark") String handleRemark,
            @Param("revisionId") Long revisionId);
}
