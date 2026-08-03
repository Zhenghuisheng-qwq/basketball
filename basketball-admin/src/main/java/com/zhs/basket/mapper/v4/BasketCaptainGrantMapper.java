package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketCaptainGrant;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketCaptainGrant
 */
public interface BasketCaptainGrantMapper {
    BasketCaptainGrant selectById(@Param("id") Long id);
    List<BasketCaptainGrant> selectList(BasketCaptainGrant query);
    int insert(BasketCaptainGrant entity);
    int updateById(BasketCaptainGrant entity);
    int softDelete(@Param("id") Long id);
}
