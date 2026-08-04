package com.zhs.basket.service.v4;

import com.zhs.basket.query.v4.BasketScheduleBatchQuery;
import com.zhs.basket.vo.v4.BasketScheduleBatchDetailVo;
import com.zhs.basket.vo.v4.BasketScheduleBatchListVo;

import java.util.List;

public interface IBasketV4ScheduleBatchService {

    List<BasketScheduleBatchListVo> selectList(BasketScheduleBatchQuery query);

    BasketScheduleBatchDetailVo selectById(Long id);

    void publish(Long id);

    void cancel(Long id);
}
