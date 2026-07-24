package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketMatchStatusLog;
import com.zhs.basket.vo.MatchStatusLogVo;

import java.util.List;

public interface BasketMatchStatusLogMapper {
    int insert(BasketMatchStatusLog log);
    List<MatchStatusLogVo> selectVoByMatchId(Long matchId);
}
