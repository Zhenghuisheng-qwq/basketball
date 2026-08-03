package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketRegistrationPlayer;
import com.zhs.basket.vo.v4.BasketRegistrationPlayerVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketRegistrationPlayerMapper {
    BasketRegistrationPlayer selectById(@Param("id") Long id);

    List<BasketRegistrationPlayer> selectList(BasketRegistrationPlayer query);

    List<BasketRegistrationPlayerVo> selectVoByRegistrationId(@Param("registrationId") Long registrationId);

    List<BasketRegistrationPlayer> selectByRegistrationId(@Param("registrationId") Long registrationId);

    int deleteByRegistrationId(@Param("registrationId") Long registrationId);

    int insert(BasketRegistrationPlayer entity);

    int batchInsert(@Param("list") List<BasketRegistrationPlayer> list);

    int updateById(BasketRegistrationPlayer entity);

    int softDelete(@Param("id") Long id);
}
