package com.zhs.dao.v4;

import com.zhs.model.v4.BasketPlayerStat;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * CNPA 赛事域 V4 Dao：BasketPlayerStat
 */
@Mapper
@Repository
public interface BasketPlayerStatDao extends BaseMapper<BasketPlayerStat> {
}
