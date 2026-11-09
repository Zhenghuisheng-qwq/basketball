package com.zhs.dao;

import com.zhs.model.BasketPageHero;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BasketPageHeroDao {
    BasketPageHero selectPublished(@Param("pageKey") String pageKey, @Param("seasonId") long seasonId);
}
