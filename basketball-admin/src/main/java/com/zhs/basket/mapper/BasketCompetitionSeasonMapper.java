package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketCompetitionSeason;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketCompetitionSeasonMapper {
    List<BasketCompetitionSeason> selectList(@Param("seasonName") String seasonName,
            @Param("seasonYear") Integer seasonYear,
            @Param("seasonStatus") Integer seasonStatus);

    BasketCompetitionSeason selectById(@Param("id") Long id);

    int insert(BasketCompetitionSeason season);

    int updateById(BasketCompetitionSeason season);

    int softDelete(@Param("id") Long id);

    int countTournamentsById(@Param("id") Long id);
}
