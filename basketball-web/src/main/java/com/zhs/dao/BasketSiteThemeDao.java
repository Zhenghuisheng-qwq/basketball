package com.zhs.dao;

import com.zhs.model.BasketSiteTheme;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BasketSiteThemeDao {
    BasketSiteTheme selectPublished(@Param("scopeType") String scopeType, @Param("scopeId") long scopeId);
}
