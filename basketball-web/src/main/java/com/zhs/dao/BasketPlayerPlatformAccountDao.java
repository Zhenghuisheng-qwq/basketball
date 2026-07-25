package com.zhs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhs.model.BasketPlayerPlatformAccount;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface BasketPlayerPlatformAccountDao extends BaseMapper<BasketPlayerPlatformAccount> {

}
