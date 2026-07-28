package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketCaptainQualification;
import com.zhs.basket.query.CaptainQualificationQuery;
import com.zhs.basket.vo.CaptainGrantSeasonOptionVo;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface BasketCaptainQualificationMapper {
    List<BasketCaptainQualification> selectList(CaptainQualificationQuery query);
    BasketCaptainQualification selectById(Long id);
    int countActiveByMemberIdAndSeasonId(@Param("memberId") Long memberId, @Param("seasonId") Long seasonId);
    int countActiveByTeamIdAndSeasonId(@Param("teamId") Long teamId, @Param("seasonId") Long seasonId);
    int insert(BasketCaptainQualification record);
    int updateById(BasketCaptainQualification record);
    int clearCurrentByMemberId(@Param("memberId") Long memberId);
    List<BasketCaptainQualification> selectActiveBySeasonId(@Param("seasonId") Long seasonId);
    List<CaptainGrantSeasonOptionVo> selectGrantableSeasonOptions();
    BasketCaptainQualification selectCurrentByPlayerId(@Param("playerId") Long playerId);
    int countCurrentActiveByPlayerId(@Param("playerId") Long playerId);
}
