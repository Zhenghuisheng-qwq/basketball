package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketMatchDataSubmissionPlayerStat;
import com.zhs.basket.vo.MatchDataSubmissionPlayerStatVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketMatchDataSubmissionPlayerStatMapper {
    int batchInsert(@Param("list") List<BasketMatchDataSubmissionPlayerStat> list);

    List<BasketMatchDataSubmissionPlayerStat> selectBySubmissionId(Long submissionId);

    List<MatchDataSubmissionPlayerStatVo> selectVoBySubmissionId(Long submissionId);
}
