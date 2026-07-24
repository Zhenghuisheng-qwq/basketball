package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketMatchPlayerStat;
import com.zhs.basket.vo.MatchPlayerStatVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface BasketMatchPlayerStatMapper {
    List<MatchPlayerStatVo> selectVoByMatchId(Long matchId);
    int softDeleteByMatchId(Long matchId);
    int batchInsert(@Param("list") List<BasketMatchPlayerStat> list);
    int updateDataStatusByMatchId(@Param("matchId") Long matchId, @Param("fromStatuses") List<Integer> fromStatuses,
            @Param("toStatus") Integer toStatus, @Param("confirmedBy") Long confirmedBy, @Param("confirmedTime") Date confirmedTime);
}
