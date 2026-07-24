package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketHonorCategory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketHonorCategoryMapper {
    BasketHonorCategory selectById(@Param("id") Long id);

    BasketHonorCategory selectByCode(@Param("honorCode") String honorCode);

    List<BasketHonorCategory> selectActiveList();
}
