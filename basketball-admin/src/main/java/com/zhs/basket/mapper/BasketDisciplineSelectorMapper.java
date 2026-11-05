package com.zhs.basket.mapper;

import com.zhs.basket.vo.DisciplineSelectorOptionVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketDisciplineSelectorMapper {

    List<DisciplineSelectorOptionVo> selectSeasonOptions(@Param("keyword") String keyword);

    List<DisciplineSelectorOptionVo> selectPlayerOptions(@Param("seasonId") Long seasonId,
            @Param("keyword") String keyword);

    List<DisciplineSelectorOptionVo> selectPermanentTeamOptions(@Param("seasonId") Long seasonId,
            @Param("keyword") String keyword);

    List<DisciplineSelectorOptionVo> selectEventTeamOptions(@Param("seasonId") Long seasonId,
            @Param("keyword") String keyword);

    List<DisciplineSelectorOptionVo> selectArchivePlayerOptions(@Param("keyword") String keyword);

    List<DisciplineSelectorOptionVo> selectArchiveTeamOptions(@Param("keyword") String keyword);
}
