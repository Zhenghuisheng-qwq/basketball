package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketMatchExceptionApplication;
import com.zhs.basket.query.MatchExceptionQuery;
import com.zhs.basket.vo.MatchExceptionListVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketMatchExceptionApplicationMapper {
    int insert(BasketMatchExceptionApplication row);

    BasketMatchExceptionApplication selectById(@Param("id") Long id);

    BasketMatchExceptionApplication selectActiveByMatchId(@Param("matchId") Long matchId);

    int countApprovedByMatchId(@Param("matchId") Long matchId);

    List<MatchExceptionListVo> selectList(MatchExceptionQuery query);

    int updateDecision(@Param("id") Long id, @Param("fromStatus") String fromStatus,
                       @Param("toStatus") String toStatus, @Param("decisionType") String decisionType,
                       @Param("decisionResult") String decisionResult,
                       @Param("decisionScoreHome") Integer decisionScoreHome,
                       @Param("decisionScoreAway") Integer decisionScoreAway,
                       @Param("winnerTeamId") Long winnerTeamId,
                       @Param("walkoverTeamId") Long walkoverTeamId,
                       @Param("processedBy") Long processedBy,
                       @Param("processedByName") String processedByName);
}
