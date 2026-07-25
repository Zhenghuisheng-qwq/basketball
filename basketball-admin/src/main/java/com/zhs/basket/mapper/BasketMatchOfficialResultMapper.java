package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketMatchOfficialResult;
import org.apache.ibatis.annotations.Param;

public interface BasketMatchOfficialResultMapper {
    BasketMatchOfficialResult selectCurrentByMatchId(@Param("matchId") Long matchId);

    int supersedeCurrent(@Param("matchId") Long matchId);

    int insert(BasketMatchOfficialResult row);
}
