package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketMatchSubmissionPlayerStat;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketMatchSubmissionPlayerStat
 */
public interface BasketMatchSubmissionPlayerStatMapper {
    BasketMatchSubmissionPlayerStat selectById(@Param("id") Long id);
    List<BasketMatchSubmissionPlayerStat> selectList(BasketMatchSubmissionPlayerStat query);
    List<BasketMatchSubmissionPlayerStat> selectBySubmissionId(@Param("submissionId") Long submissionId);
    int deleteBySubmissionId(@Param("submissionId") Long submissionId);
    int insert(BasketMatchSubmissionPlayerStat entity);
    int updateById(BasketMatchSubmissionPlayerStat entity);
    int softDelete(@Param("id") Long id);

    int countByDivisionId(@Param("divisionId") Long divisionId);

    int countByEventPlayerId(@Param("eventPlayerId") Long eventPlayerId);
}
