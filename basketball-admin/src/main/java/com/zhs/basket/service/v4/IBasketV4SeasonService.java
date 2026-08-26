package com.zhs.basket.service.v4;

import com.zhs.basket.dto.v4.BasketSeasonCreateReq;
import com.zhs.basket.dto.v4.BasketSeasonUpdateReq;
import com.zhs.basket.query.v4.BasketSeasonQuery;
import com.zhs.basket.vo.v4.BasketSeasonDetailVo;
import com.zhs.basket.vo.v4.BasketSeasonListVo;

import java.util.List;

/**
 * CNPA 赛事域 V4：Season 发布主链路。
 */
public interface IBasketV4SeasonService {

    List<BasketSeasonListVo> selectList(BasketSeasonQuery query);

    BasketSeasonDetailVo selectById(Long id);

    Long create(BasketSeasonCreateReq req);

    void update(Long id, BasketSeasonUpdateReq req);

    void publish(Long id);

    void offline(Long id);

    void delete(Long id);

    void refreshAggregatedStatus(Long seasonId);
}

