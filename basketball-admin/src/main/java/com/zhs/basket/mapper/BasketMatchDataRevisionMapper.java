package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketMatchDataRevision;
import org.apache.ibatis.annotations.Param;

public interface BasketMatchDataRevisionMapper {
    int insert(BasketMatchDataRevision revision);

    BasketMatchDataRevision selectById(@Param("id") Long id);
}
