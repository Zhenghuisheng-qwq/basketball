package com.zhs.dao.v4;

import com.zhs.model.v4.BasketSeries;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * CNPA 赛事域 V4 Dao：BasketSeries
 */
@Mapper
@Repository
public interface BasketSeriesDao extends BaseMapper<BasketSeries> {
}
