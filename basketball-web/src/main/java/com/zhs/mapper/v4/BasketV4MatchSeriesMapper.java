package com.zhs.mapper.v4;

import com.zhs.model.v4.BasketMatchSeries;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface BasketV4MatchSeriesMapper {

    BasketMatchSeries selectById(@Param("id") Long id);

    List<BasketMatchSeries> selectByStageId(@Param("stageId") Long stageId);

    List<BasketMatchSeries> selectByDivisionId(@Param("divisionId") Long divisionId);
}
