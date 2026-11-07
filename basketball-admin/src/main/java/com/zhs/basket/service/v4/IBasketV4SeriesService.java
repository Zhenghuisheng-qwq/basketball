package com.zhs.basket.service.v4;

import com.zhs.basket.vo.v4.BasketSeriesVo;

import java.util.List;

/**
 * CNPA 赛事域 V4：Series 查询与启停。
 */
public interface IBasketV4SeriesService {

    List<BasketSeriesVo> selectList(Integer enabled);

    List<BasketSeriesVo> selectHonorIconList();

    BasketSeriesVo selectById(Long id);

    void updateEnabled(Long id, Integer enabled);

    void updateHonorIconUrl(Long id, String honorIconUrl);
}
