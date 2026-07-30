package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketMatchDataSubmissionPeriod;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketMatchDataSubmissionPeriodMapper {
    int batchInsert(@Param("list") List<BasketMatchDataSubmissionPeriod> list);

    List<BasketMatchDataSubmissionPeriod> selectBySubmissionId(Long submissionId);
}
