package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketMatchDataDispute;
import com.zhs.basket.query.MatchDisputeQuery;
import com.zhs.basket.vo.MatchDisputeListVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketMatchDataDisputeMapper {
    int insert(BasketMatchDataDispute row);

    int countPendingByMatchAndType(@Param("matchId") Long matchId, @Param("disputeType") String disputeType);

    int countActiveByMatchId(@Param("matchId") Long matchId);

    BasketMatchDataDispute selectActiveByMatchId(@Param("matchId") Long matchId);

    List<MatchDisputeListVo> selectList(MatchDisputeQuery query);

    BasketMatchDataDispute selectById(Long id);

    int updateHandle(@Param("id") Long id, @Param("fromStatus") String fromStatus,
            @Param("toStatus") String toStatus, @Param("handlerId") Long handlerId,
            @Param("handlerName") String handlerName,
            @Param("handleResult") String handleResult, @Param("handleRemark") String handleRemark,
            @Param("rejectReason") String rejectReason,
            @Param("revisionId") Long revisionId);
}
