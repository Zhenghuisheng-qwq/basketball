package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketMatchSubmissionTeamStat;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketMatchSubmissionTeamStat
 */
public interface BasketMatchSubmissionTeamStatMapper {
    BasketMatchSubmissionTeamStat selectById(@Param("id") Long id);
    List<BasketMatchSubmissionTeamStat> selectList(BasketMatchSubmissionTeamStat query);
    int insert(BasketMatchSubmissionTeamStat entity);
    int updateById(BasketMatchSubmissionTeamStat entity);
    int softDelete(@Param("id") Long id);
}
