package com.zhs.basket.service.v4;

import com.zhs.basket.query.v4.BasketEventPlayerQuery;
import com.zhs.basket.vo.v4.BasketEventPlayerListVo;

import java.util.List;

public interface IBasketV4EventPlayerService {
    List<BasketEventPlayerListVo> selectList(BasketEventPlayerQuery query);
}
