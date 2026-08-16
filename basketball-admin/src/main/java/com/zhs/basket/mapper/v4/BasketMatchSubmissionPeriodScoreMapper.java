package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketMatchSubmissionPeriodScore;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketMatchSubmissionPeriodScoreMapper {
    List<BasketMatchSubmissionPeriodScore> selectBySubmissionId(@Param("submissionId") Long submissionId);
    int insert(BasketMatchSubmissionPeriodScore entity);
}
