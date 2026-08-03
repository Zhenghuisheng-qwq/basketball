package com.zhs.basket.service.v4;

import com.zhs.basket.query.v4.BasketEventTeamQuery;
import com.zhs.basket.vo.v4.BasketEventTeamListVo;

import java.util.List;

public interface IBasketV4EventTeamService {
    List<BasketEventTeamListVo> selectList(BasketEventTeamQuery query);

    BasketEventTeamListVo selectById(Long id);
}
