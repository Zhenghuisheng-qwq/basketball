package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketMatchPeriodScore;
import com.zhs.basket.vo.MatchPeriodScoreVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketMatchPeriodScoreMapper {
    List<MatchPeriodScoreVo> selectVoByMatchId(Long matchId);
    int softDeleteByMatchId(Long matchId);
    int batchInsert(@Param("list") List<BasketMatchPeriodScore> list);
}
