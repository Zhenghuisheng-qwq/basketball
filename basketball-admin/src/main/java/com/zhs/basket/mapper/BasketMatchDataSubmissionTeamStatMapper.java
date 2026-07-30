package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketMatchDataSubmissionTeamStat;
import com.zhs.basket.vo.MatchDataSubmissionTeamStatVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketMatchDataSubmissionTeamStatMapper {
    int batchInsert(@Param("list") List<BasketMatchDataSubmissionTeamStat> list);

    List<BasketMatchDataSubmissionTeamStat> selectBySubmissionId(Long submissionId);

    List<MatchDataSubmissionTeamStatVo> selectVoBySubmissionId(Long submissionId);
}
